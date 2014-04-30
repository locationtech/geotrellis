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
import geotrellis.spark.ingest.IngestInputFormat
import geotrellis.spark.ingest.TiffTiler
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.rdd.RasterSplitGenerator
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.HdfsUtils
import geotrellis.spark.utils.SparkUtils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile
import org.apache.spark.Logging
import org.apache.spark.SerializableWritable
import org.apache.spark.SparkContext._
import org.geotools.coverage.grid.GridCoverage2D

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.FieldArgs
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

object Ingest extends ArgMain[IngestArgs] with Logging {

  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def main(args: IngestArgs) {
    val inPath = new Path(args.input)
    val outPath = new Path(args.outputpyramid)
    val sparkMaster = args.sparkMaster
    val runLocal = sparkMaster == null

    val hadoopConf = args.hadoopConf

    logInfo(s"Deleting and creating output path: $outPath")
    val outFs = outPath.getFileSystem(hadoopConf)
    outFs.delete(outPath, true)
    outFs.mkdirs(outPath)

    if (runLocal) {
      val (files, meta) = PyramidMetadata.fromTifFiles(inPath, hadoopConf)
      logInfo("------- FILES ------")
      logInfo(files.mkString("\n"))
      logInfo("\n\n\n")
      logInfo("------- META ------")
      logInfo(meta.toString)
      meta.save(outPath, hadoopConf)

      val tiles = files.flatMap(file => TiffTiler.tile(file, meta, hadoopConf))

      val outPathWithZoom = createZoomDirectory(outPath, meta.maxZoomLevel, outFs)

      val partitioner = createPartitioner(outPathWithZoom, meta, hadoopConf)

      val key = new TileIdWritable()

      // open as many writers as number of partitions
      def openWriters(num: Int) = {
        val writers = new Array[MapFile.Writer](num)
        for (i <- 0 until num) {
          val mapFilePath = new Path(outPathWithZoom, f"part-${i}%05d")

          writers(i) = new MapFile.Writer(hadoopConf, outFs, mapFilePath.toUri.toString,
            classOf[TileIdWritable], classOf[ArgWritable], SequenceFile.CompressionType.RECORD)

        }
        writers
      }

      setOutputParameters(hadoopConf)
      val writers = openWriters(partitioner.numPartitions)
      try {
        tiles.foreach {
          case (tileId, tile) => {
            key.set(tileId)
            writers(partitioner.getPartition(key)).append(key, ArgWritable.fromRasterData(tile.toArrayRaster.data))
            //logInfo(s"Saved tileId=${tileId},partition=${partitioner.getPartition(key)}")
          }
        }
      }
      finally {
        writers.foreach(_.close)
      }
      logInfo(s"Done saving ${tiles.length} tiles")
    }

    else {
      // run on spark
      val sc = args.sparkContext("Ingest")

      try {
        val (files, meta) = PyramidMetadata.fromTifFiles(inPath, hadoopConf, sc)

        logInfo("------- META ------")
        logInfo(meta.toString)
        meta.save(outPath, hadoopConf)
        meta.writeToJobConf(hadoopConf)

        // if less than 10 input files, print them out
        if (files.length < 10) {
          logInfo("------- FILES ------")
          logInfo(files.mkString("\n"))
        }

        val outPathWithZoom = createZoomDirectory(outPath, meta.maxZoomLevel, outFs)

        val partitioner = createPartitioner(outPathWithZoom, meta, hadoopConf)

        val newConf = HdfsUtils.putFilesInConf(files.mkString(","), hadoopConf)
        val rdd = sc.newAPIHadoopRDD(newConf, classOf[IngestInputFormat], classOf[Long], classOf[Raster])
        setOutputParameters(newConf)

        val broadCastedConf = sc.broadcast(new SerializableWritable(newConf))
        val outPathWithZoomStr = outPathWithZoom.toUri().toString()
        val res =
          rdd.map(t => Tile(t._1, t._2).toWritable)
            .partitionBy(partitioner)
            .reduceByKey((tile1, tile2) => tile2) // pick the later one
            .mapPartitionsWithIndex({ (index, iter) =>
              {
                val conf = broadCastedConf.value.value
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
              }
            }, true)
        logInfo(s"Done saving ${res.count()} tiles")
      }
      finally {
        sc.stop
      }
    }
  }

  // mutates hadoopConf
  private def setOutputParameters(conf: Configuration): Unit = {
    conf.set("io.map.index.interval", "1")
    //conf.setInt("dfs.replication", 1)
  }
  
  private def createPartitioner(rasterPath: Path, meta: PyramidMetadata, conf: Configuration): TileIdPartitioner = {
    val tileExtent = meta.metadataForBaseZoom.tileExtent
    val (zoom, tileSize, rasterType) = (meta.maxZoomLevel, meta.tileSize, meta.rasterType)
    val tileSizeBytes = TmsTiling.tileSizeBytes(tileSize, rasterType)
    val blockSizeBytes = HdfsUtils.defaultBlockSize(rasterPath, conf)
    val splitGenerator = RasterSplitGenerator(tileExtent, zoom, tileSizeBytes, blockSizeBytes)

    val partitioner = TileIdPartitioner(splitGenerator, rasterPath, conf)

    logInfo(s"SplitGenerator params (tileSize,blockSize,increment) = (${tileSizeBytes}, ${blockSizeBytes}," + 
            s"${RasterSplitGenerator.computeIncrement(tileExtent, tileSizeBytes, blockSizeBytes)}")
    logInfo(s"Saving splits: " + partitioner)
    partitioner
  }
  
  private def createZoomDirectory(pyramid: Path, zoom: Int, fs: FileSystem): Path = {
    val outPathWithZoom = new Path(pyramid, zoom.toString)
    logInfo(s"Creating Output Path With Zoom: $outPathWithZoom")
    fs.mkdirs(outPathWithZoom)
    outPathWithZoom
  }

  /* debugging only */
  private def inspect(tile: GridCoverage2D): Unit = {
    val env = tile.getEnvelope()
    val r = tile.getRenderedImage().getData()
    for {
      py <- 0 until r.getHeight()
      if (py < 10)
      px <- 0 until r.getWidth()
      if (px < 10)
    } {
      val d = r.getSampleFloat(px, py, 0)
      println(s"($px,$py)=$d")
    }
  }
}
