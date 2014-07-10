/*
 * Copyright (c) 2014 DigitalGlobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.cmd

import geotrellis._
import geotrellis.spark.Tile
import geotrellis.spark.cmd.args.HadoopArgs
import geotrellis.spark.cmd.args.SparkArgs
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.ingest.GeoTiff
import geotrellis.spark.ingest.IngestInputFormat
import geotrellis.spark.ingest.MetadataInputFormat
import geotrellis.spark.ingest.TiffTiler
import geotrellis.spark.metadata.JacksonWrapper
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.rdd.RasterSplitGenerator
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.HdfsUtils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile
import org.apache.spark.Logging
import org.apache.spark.SerializableWritable
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

import java.io.PrintWriter

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

/**
 * @author akini
 *
 * Ingest GeoTIFFs into ArgWritable.
 *
 * Works in two modes:
 *
 * Local - all processing is done on a single node in RAM and not using Spark. Use this if
 * ingesting a single file or a bunch of files that do not overlap. Also, all files in
 * aggregate must fit in RAM. The non-overlapping constraint is due to there not being
 * any mosaicing in local mode
 *
 * Command for local mode:
 * Ingest --input <path-to-tiffs> --outputpyramid <path-to-pyramid>
 * e.g., Ingest --input file:///home/akini/test/small_files/all-ones.tif --output file:///tmp/all-ones
 *
 * Spark - all processing is done in Spark. Use this if ingesting multiple files, which in
 * aggregate do not fit on a single node's RAM. Or multiple files which may overlap.
 *
 * Constraints:
 *
 * --input <path-to-tiffs> - this can either be a directory or a single tiff file and can either be in local fs or hdfs
 *
 * --outputpyramid <path-to-raster> - this can be either on hdfs (hdfs://) or local fs (file://). If the directory
 * already exists, it is deleted
 *
 *
 */

class IngestArgs extends SparkArgs with HadoopArgs {
  @Required var input: String = _
  @Required var outputpyramid: String = _
}

case class IngestPaths(inPath: Path, outPath: Path, outFs: FileSystem)

case class IngestData(metadata: PyramidMetadata, files: Seq[Path])

object IngestCommand extends ArgMain[IngestArgs] with Logging {

  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def main(args: IngestArgs): Unit = {
    val hadoopConf = args.hadoopConf

    val inPath = new Path(args.input)
    val outPath = new Path(args.outputpyramid)

    val sc = if (args.sparkMaster == null) null else args.sparkContext("Ingest")
    ingest(inPath, outPath, hadoopConf, sc)
  }

  def ingest(input: Path, output: Path, conf: Configuration, sc: SparkContext) {
    conf.set("io.map.index.interval", "1")
    logInfo(s"Deleting and creating output path: $output")
    val outFs: FileSystem = output.getFileSystem(conf)
    outFs.delete(output, true)
    outFs.mkdirs(output)

    val paths = IngestPaths(input, output, outFs)
    if (sc == null)
      new LocalIngest(conf).ingest(paths)
    else
      new SparkIngest(conf, sc).ingest(paths)
  }
}

abstract class Ingest(hadoopConf: Configuration) extends Logging with Serializable {
  protected def doIngest(ingestData: IngestData, outPathWithZoom: Path, partitioner: TileIdPartitioner, ingestPaths: IngestPaths): Unit

  protected def getIngestData(files: List[Path]): IngestData

  def ingest(paths: IngestPaths): Unit = {
    val allFiles = HdfsUtils.listFiles(paths.inPath, hadoopConf)

    val ingestData = getIngestData(allFiles)

    // Save pyramid metadata
    val metaPath = new Path(paths.outPath, PyramidMetadata.MetaFile)
    val fs = metaPath.getFileSystem(hadoopConf)
    val fdos = fs.create(metaPath)
    val out = new PrintWriter(fdos)
    out.println(JacksonWrapper.prettyPrint(ingestData.metadata))
    out.close()
    fdos.close()

    logInfo("------- META ------")
    logInfo(ingestData.metadata.toString)

    logInfo("------- FILES ------")
    if (ingestData.files.length < 10) {
      // if less than 10 input files, print them out
      logInfo(ingestData.files.mkString("\n"))
    } else {
      logInfo((ingestData.files.take(10) ++ Seq("...")) mkString ("\n"))
    }

    val outPathWithZoom = createZoomDirectory(paths.outPath, ingestData.metadata.maxZoomLevel, paths.outFs)
    val partitioner = createPartitioner(outPathWithZoom, ingestData.metadata, hadoopConf)

    doIngest(ingestData, outPathWithZoom, partitioner, paths)
  }

  def createPartitioner(rasterPath: Path, meta: PyramidMetadata, hadoopConf: Configuration): TileIdPartitioner = {
    val tileExtent = meta.metadataForBaseZoom.tileExtent
    val (zoom, tileSize, rasterType) = (meta.maxZoomLevel, meta.tileSize, meta.rasterType)
    val tileSizeBytes = TmsTiling.tileSizeBytes(tileSize, rasterType)
    val blockSizeBytes = HdfsUtils.defaultBlockSize(rasterPath, hadoopConf)
    val splitGenerator = RasterSplitGenerator(tileExtent, zoom, tileSizeBytes, blockSizeBytes)

    val partitioner = TileIdPartitioner(splitGenerator, rasterPath, hadoopConf)

    logInfo(s"SplitGenerator params (tileSize,blockSize,increment) = (${tileSizeBytes}, ${blockSizeBytes}," +
      s"${RasterSplitGenerator.computeIncrement(tileExtent, tileSizeBytes, blockSizeBytes)}")
    logInfo(s"Saving splits: " + partitioner)
    partitioner
  }

  def createZoomDirectory(pyramid: Path, zoom: Int, fs: FileSystem): Path = {
    val outPathWithZoom = new Path(pyramid, zoom.toString)
    logInfo(s"Creating Output Path With Zoom: $outPathWithZoom")
    fs.mkdirs(outPathWithZoom)
    outPathWithZoom
  }
}

object SparkIngest extends Logging {
  /** Pulled out into the companion object for spark execution.*/
  def createRdd(
    rdd: RDD[(Long, Raster)],
    partitioner: TileIdPartitioner,
    broadcastedConf: Broadcast[SerializableWritable[Configuration]],
    outPathWithZoomStr: String): RDD[(TileIdWritable, ArgWritable)] = {
    rdd.map(t => Tile(t._1, t._2).toWritable)
      .partitionBy(partitioner)
      .reduceByKey((tile1, tile2) => tile2) // pick the later one
      .mapPartitionsWithIndex({ (index, iter) =>
        val conf = broadcastedConf.value.value
        val buf = iter.toArray.sortWith((x, y) => x._1.get() < y._1.get())

        val mapFilePath = new Path(outPathWithZoomStr, f"part-${index}%05d")
        val fs = mapFilePath.getFileSystem(conf)
        val fsRep = fs.getDefaultReplication()
        logInfo(s"Working on partition ${index} with rep = (${conf.getInt("dfs.replication", -1)}, ${fsRep})")
        val writer = new MapFile.Writer(conf, fs, mapFilePath.toUri.toString,
          classOf[TileIdWritable], classOf[ArgWritable], SequenceFile.CompressionType.RECORD)
        buf.foreach(writableTile => writer.append(writableTile._1, writableTile._2))
        writer.close()
        iter
      }, true)
  }
}

class SparkIngest(hadoopConf: Configuration, sparkContext: SparkContext) extends Ingest(hadoopConf) {

  def getIngestData(allFiles: List[Path]): IngestData = {
    val newConf = HdfsUtils.putFilesInConf(allFiles.mkString(","), hadoopConf)

    val (acceptedFiles, optMetas) =
      sparkContext
        .newAPIHadoopRDD(
          newConf,
          classOf[MetadataInputFormat],
          classOf[String],
          classOf[Option[GeoTiff.Metadata]])
        .collect
        .unzip

    val files = acceptedFiles.map(new Path(_))
    val meta = optMetas.flatten.reduceLeft(_.merge(_))
    IngestData(PyramidMetadata.fromGeoTiffMeta(meta), files)
  }

  def doIngest(ingestData: IngestData, outPathWithZoom: Path, partitioner: TileIdPartitioner, ingestPaths: IngestPaths): Unit = {
    val IngestData(meta, files) = ingestData

    try {
      meta.writeToJobConf(hadoopConf)
      val newConf = HdfsUtils.putFilesInConf(files.mkString(","), hadoopConf)
      val rdd = sparkContext.newAPIHadoopRDD(newConf, classOf[IngestInputFormat], classOf[Long], classOf[Raster])

      val broadcastedConf = sparkContext.broadcast(new SerializableWritable(newConf))

      // Turn into a string because Path type is not serializable.
      val outPathWithZoomStr = outPathWithZoom.toUri().toString()

      val res = SparkIngest.createRdd(rdd, partitioner, broadcastedConf, outPathWithZoomStr)
      logInfo(s"Done saving ${res.count()} tiles")
    } finally {
      sparkContext.stop
    }
  }
}

class LocalIngest(hadoopConf: Configuration) extends Ingest(hadoopConf) {

  def getIngestData(allFiles: List[Path]): IngestData = {
    val (files, optMetas) =
      allFiles
        .map { file =>
          val meta = GeoTiff.getMetadata(file, hadoopConf)
          (file, meta)
        }
        .filter { case (file, meta) => meta.isDefined }
        .unzip

    val meta = optMetas.flatten.reduceLeft(_.merge(_))

    IngestData(PyramidMetadata.fromGeoTiffMeta(meta), files)
  }

  def doIngest(ingestData: IngestData, outPathWithZoom: Path, partitioner: TileIdPartitioner, ingestPaths: IngestPaths): Unit = {
    val IngestData(meta, files) = ingestData

    val key = new TileIdWritable()

    val tiles: Seq[(Long, Raster)] =
      files
        .map(TiffTiler.tile(_, meta, hadoopConf))
        .flatten

    // open as many writers as number of partitions
    val writers = {
      val num = partitioner.numPartitions
      val writers = new Array[MapFile.Writer](num)
      for (i <- 0 until num) {
        val mapFilePath = new Path(outPathWithZoom, f"part-${i}%05d")

        writers(i) = new MapFile.Writer(hadoopConf, ingestPaths.outFs, mapFilePath.toUri.toString,
          classOf[TileIdWritable], classOf[ArgWritable], SequenceFile.CompressionType.RECORD)
      }
      writers
    }

    try {
      for ((tileId, tile) <- tiles) {
        key.set(tileId)
        val partition = partitioner.getPartition(key)
        val writer = writers(partition)
        val argWritable = ArgWritable.fromRasterData(tile.toArrayRaster.data)

        writer.append(key, argWritable)
        logInfo(s"Saved tileId=${tileId},partition=${partitioner.getPartition(key)}")
      }
    } finally {
      writers.foreach(_.close)
    }

    logInfo(s"Done saving ${tiles.length} tiles")
  }
}
