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

import geotrellis.raster._
import geotrellis.vector.Extent

import geotrellis.spark.TmsTile
import geotrellis.spark.cmd.args._
import geotrellis.spark.formats._
import geotrellis.spark.ingest._
import geotrellis.spark.metadata._
import geotrellis.spark.tiling._
import geotrellis.spark.rdd._
import geotrellis.spark.utils.HdfsUtils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd._

import java.io.PrintWriter

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

import spire.syntax.cfor._

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
    hadoopConf.set("io.map.index.interval", "1")

    val inPath = new Path(args.input)
    val outPath = new Path(args.outputpyramid)

    logInfo(s"Deleting and creating output path: $outPath")
    val outFs: FileSystem = outPath.getFileSystem(hadoopConf)
    outFs.delete(outPath, true)
    outFs.mkdirs(outPath)

    if (args.sparkMaster == null)
      ingest(inPath, outPath, hadoopConf)
    else
      newIngest(inPath, outPath, hadoopConf, args.sparkContext("Ingest"))
  }

  def newIngest(input: Path, output: Path, conf: Configuration, sc: SparkContext): Unit = {
    case class TileInfo(tileId: Long, extent: Extent)

    //(tileScheme, crs).tileIdsForExtent(extent)
    def tileIdsForExtent(extent: Extent): Seq[TileInfo] = {
      val zoom = TmsTiling.MaxZoomLevel
      val tileSize = TmsTiling.DefaultTileSize
      val tileExtent = 
        TmsTiling.extentToTile(
          extent,
          zoom,
          tileSize
        )
      val tileInfos =
        for { tcol <- tileExtent.xmin to tileExtent.xmax;
              trow <- tileExtent.ymin to tileExtent.ymax } yield {
          val id = TmsTiling.tileId(tcol, trow, zoom)
          val extent = TmsTiling.tileToExtent(tcol, trow, zoom, tileSize)
          TileInfo(id, extent)
        }

      tileInfos.toSeq
    }

    implicit class ValueBurner(val tile: MutableArrayTile) {
      def burnValues(other: Tile): MutableArrayTile = {
        Seq(tile, other).assertEqualDimensions
        if(tile.cellType.isFloatingPoint) {
          cfor(0)(_ < tile.rows, _ + 1) { row =>
            cfor(0)(_ < tile.cols, _ + 1) { col =>
              if(isNoData(tile.getDouble(col, row))) { 
                tile.setDouble(col, row, other.getDouble(col, row)) 
              }
            }
          }
        } else {
          cfor(0)(_ < tile.rows, _ + 1) { row =>
            cfor(0)(_ < tile.cols, _ + 1) { col =>
              if(isNoData(tile.get(col, row))) { 
                tile.setDouble(col, row, other.get(col, row)) 
              }
            }
          }
        }

        tile
      }

      def burnValues(extent: Extent, otherExtent: Extent, other: Tile): MutableArrayTile = {
        val re = RasterExtent(extent, tile.cols, tile.rows)
        val GridBounds(colMin, colMax, rowMin, rowMax) = re.gridBoundsFor(otherExtent)
        val otherRe = RasterExtent(otherExtent, other.cols, other.rows)

        def thisToOther(col: Int, row: Int): (Int, Int) = {
          val (x, y) = re.gridToMap(col, row)
          otherRe.mapToGrid(x, y)
        }

        if(tile.cellType.isFloatingPoint) {
          cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
            cfor(colMin)(_ <= colMax, _ + 1) { col =>
              if(isNoData(tile.getDouble(col, row))) {
                val (otherCol, otherRow) = thisToOther(col, row)
                tile.setDouble(col, row, other.getDouble(otherCol, otherRow))
              }
            }
          }
        } else {
          cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
            cfor(colMin)(_ <= colMax, _ + 1) { col =>
              if(isNoData(tile.get(col, row))) {
                val (otherCol, otherRow) = thisToOther(col, row)
                tile.set(col, row, other.get(otherCol, otherRow))
              }
            }
          }

        }

        tile
      }
    }

    val partitioner: Partitioner = ???

    //////////////////////

    val allFiles = HdfsUtils.listFiles(input, conf)
    val newConf = HdfsUtils.putFilesInConf(allFiles.mkString(","), conf)

    val geotiffRdd = 
      sc.newAPIHadoopRDD(newConf, classOf[GeotiffInputFormat], classOf[Extent], classOf[Tile])

    val (extent, cellType, cellSize): (Extent, CellType, CellSize) =
      geotiffRdd
        .map { case (extent, tile) => (extent, tile.cellType, CellSize(extent, tile.cols, tile.rows)) }
        .reduce { (t1, t2) =>
          val (e1, ct1, cs1) = t1
          val (e2, ct2, cs2) = t2
          (e1.combine(e2), ct1.union(ct2),
            if(cs1.resolution < cs2.resolution) cs1 else cs2
          )
         }

    val tileScheme: TilingScheme = DefaultTilingScheme
    val zoomLevel: ZoomLevel = tileScheme.zoomLevelFor(cellSize)

    val bcZoomLevel = sc.broadcast(zoomLevel)

    val tiles: RDD[TmsTile] =
      geotiffRdd
        .flatMap { case (extent, tile) =>
          val zoomLevel = bcZoomLevel.value
          zoomLevel.tileIdsForExtent(extent).map { case tileId  => (tileId, (tileId, extent, tile)) }
         }
        .combineByKey( 
          { case (tileId, extent, tile) =>
            val zoomLevel = bcZoomLevel.value
            val tmsTile = ArrayTile.empty(cellType, zoomLevel.tileCols, zoomLevel.tileRows)
            tmsTile.burnValues(zoomLevel.extentForTile(tileId), extent, tile)
          },
          { (tmsTile: MutableArrayTile, tup: (Long, Extent, Tile)) =>
            val zoomLevel = bcZoomLevel.value
            val (tileId, extent, tile) = tup
            tmsTile.burnValues(zoomLevel.extentForTile(tileId), extent, tile)
          },
          { (tmsTile1: MutableArrayTile , tmsTile2: MutableArrayTile) =>
            tmsTile1.burnValues(tmsTile2)
          }
         )
        .map { case (id, tile) => TmsTile(id, tile) }

     tiles
       .partitionBy(partitioner)
       .withContext(???)
       .save(output)

  }

  def ingest(input: Path, output: Path, conf: Configuration): Unit = {
    val outFs: FileSystem = output.getFileSystem(conf)

    val paths = IngestPaths(input, output, outFs)

    val allFiles = HdfsUtils.listFiles(paths.inPath, conf)
    val newConf = HdfsUtils.putFilesInConf(allFiles.mkString(","), conf)

    new LocalIngest(conf).ingest(paths)
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
    val (zoom, tileSize, cellType) = (meta.maxZoomLevel, meta.tileSize, meta.cellType)
    val tileSizeBytes = TmsTiling.tileSizeBytes(tileSize, cellType)
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
    rdd: RDD[(Long, Tile)],
    partitioner: TileIdPartitioner,
    broadcastedConf: Broadcast[SerializableWritable[Configuration]],
    outPathWithZoomStr: String): RDD[(TileIdWritable, ArgWritable)] = {
    rdd.map(t => TmsTile(t._1, t._2).toWritable)
      .partitionBy(partitioner)
      .reduceByKey((tile1, tile2) => tile2) // pick the later one
      .mapPartitionsWithIndex({ (index, tmsTiles) =>
        val conf = broadcastedConf.value.value
        val sortedTiles = tmsTiles.toArray.sortWith((x, y) => x._1.get() < y._1.get())

        val mapFilePath = new Path(outPathWithZoomStr, f"part-${index}%05d")
        val fs = mapFilePath.getFileSystem(conf)
        val fsRep = fs.getDefaultReplication(mapFilePath)
        logInfo(s"Working on partition ${index} with rep = (${conf.getInt("dfs.replication", -1)}, ${fsRep})")
        val writer = new MapFile.Writer(conf, fs, mapFilePath.toUri.toString,
          classOf[TileIdWritable], classOf[ArgWritable], SequenceFile.CompressionType.RECORD)

        for( (id, arg) <- sortedTiles) {
          writer.append(id, arg)
        }

        writer.close()
        tmsTiles
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
      val rdd = sparkContext.newAPIHadoopRDD(newConf, classOf[IngestInputFormat], classOf[Long], classOf[Tile])

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

    val tiles: Seq[(Long, Tile)] =
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
        val argWritable = ArgWritable.fromTile(tile.toArrayTile)

        writer.append(key, argWritable)
        logInfo(s"Saved tileId=${tileId},partition=${partitioner.getPartition(key)}")
      }
    } finally {
      writers.foreach(_.close)
    }

    logInfo(s"Done saving ${tiles.length} tiles")
  }
}
