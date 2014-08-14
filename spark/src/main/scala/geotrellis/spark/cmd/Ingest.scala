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
import geotrellis.vector._

import geotrellis.spark._
import geotrellis.spark.cmd.args._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
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
import scala.language.reflectiveCalls

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
  * Constraints:
  *
  * --input <path-to-tiffs> - this can either be a directory or a single tiff file and can either be in local fs or hdfs
  *
  * --outputpyramid <path-to-raster> - this can be either on hdfs (hdfs://) or local fs (file://). If the directory
  * already exists, it is deleted
  * 
  * --sparkMaster <spark-name>   i.e. local[10]
  *
  */

class IngestArgs extends SparkArgs with HadoopArgs {
  @Required var input: String = _
  @Required var outputpyramid: String = _
}

case class RasterMetaData(cellType: CellType, extent: Extent, zoomLevel: ZoomLevel)

object Ingest { def apply(sc: SparkContext): Ingest = new Ingest(sc) }

class Ingest(sc: SparkContext) {
  type Sink = (RDD[TmsTile], RasterMetaData)=>Unit

  def setMetaData(sourceTiles: RDD[(Extent, Tile)]): (RDD[(Extent, Tile)], RasterMetaData) =  {
    val (uncappedExtent, cellType, cellSize): (Extent, CellType, CellSize) =
      sourceTiles
        .map { case (extent, tile) => (extent, tile.cellType, CellSize(extent, tile.cols, tile.rows)) }
        .reduce { (t1, t2) =>
        val (e1, ct1, cs1) = t1
        val (e2, ct2, cs2) = t2
        (e1.combine(e2), ct1.union(ct2),
          if(cs1.resolution < cs2.resolution) cs1 else cs2
        )
      }

    val tileScheme: TilingScheme = TilingScheme.GEODETIC
    val zoomLevel: ZoomLevel = tileScheme.zoomLevelFor(cellSize)

    val extent = tileScheme.extent.intersection(uncappedExtent).get
    (sourceTiles, RasterMetaData(cellType, extent, zoomLevel))
  }

  def mosaic(sourceTiles: RDD[(Extent, Tile)], metaData: RasterMetaData): (RDD[TmsTile], RasterMetaData) = {
    val bcZoomLevel = sc.broadcast(metaData.zoomLevel)
    val tiles =
      sourceTiles
        .flatMap { case (extent, tile) =>
          val zoomLevel = bcZoomLevel.value
          zoomLevel.tileIdsForExtent(extent).map { case tileId  => (tileId, (tileId, extent, tile)) }
         }
        .combineByKey( 
          { case (tileId, extent, tile) =>
            val zoomLevel = bcZoomLevel.value
            val tmsTile = ArrayTile.empty(tile.cellType, zoomLevel.pixelCols, zoomLevel.pixelRows)
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
    (tiles, metaData)
  }

  def apply(source: =>RDD[(Extent, Tile)], sink:  Sink): Unit =
    source |>
    setMetaData |>
    mosaic |>
    sink
}

object HadoopIngestCommand extends ArgMain[IngestArgs] with Logging {

  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def main(args: IngestArgs): Unit = {
    val conf = args.hadoopConf
    conf.set("io.map.index.interval", "1")

    val inPath = new Path(args.input)
    val outPath = new Path(args.outputpyramid)

    logInfo(s"Deleting and creating output path: $outPath")
    val outFs: FileSystem = outPath.getFileSystem(conf)
    outFs.delete(outPath, true)
    outFs.mkdirs(outPath)

    val sparkContext = args.sparkContext("Ingest")
    try {
      val source = sparkContext.hadoopGeoTiffRDD(inPath)
      val sink = { (tiles: RDD[TmsTile], metaData: RasterMetaData) =>
        val RasterMetaData(cellType, extent, zoomLevel) = metaData
        val tileExtent = zoomLevel.tileExtentForExtent(extent)

        val tileSizeBytes = TmsTiling.tileSizeBytes(zoomLevel.tileSize, cellType)
        val blockSizeBytes = HdfsUtils.defaultBlockSize(inPath, conf)

        val splitGenerator = RasterSplitGenerator(tileExtent, zoomLevel.level, tileSizeBytes, blockSizeBytes)
        val partitioner = TileIdPartitioner(splitGenerator.splits)


        val rasterMetadata =
          RasterMetadata(TmsTiling.extentToPixel(extent, zoomLevel.level, zoomLevel.tileSize), tileExtent)

        val meta: PyramidMetadata =
          PyramidMetadata(
            extent,
            zoomLevel.tileSize,
            1, // bands
            Byte.MinValue.toDouble,
            CellType.toAwtType(cellType),
            zoomLevel.level,
            Map(zoomLevel.level.toString -> rasterMetadata))

        val context: Context =
          Context(zoomLevel.level, meta, partitioner)

        val outPathWithZoom = new Path(outPath, zoomLevel.level.toString)

        tiles
          .partitionBy(partitioner)
          .withContext(context)
          .saveAsHadoopRasterRDD(outPathWithZoom)

        logInfo(s"Saved raster at zoom level ${zoomLevel.level} to $outPathWithZoom")
      }

      Ingest(sparkContext)(source, sink)

    } finally {
      sparkContext.stop
    }
  }
}


// object IngestCommand extends ArgMain[IngestArgs] with Logging {

//   System.setProperty("com.sun.media.jai.disableMediaLib", "true")

//   def main(args: IngestArgs): Unit = {
//     val conf = args.hadoopConf
//     conf.set("io.map.index.interval", "1")

//     val inPath = new Path(args.input)
//     val outPath = new Path(args.outputpyramid)

//     logInfo(s"Deleting and creating output path: $outPath")
//     val outFs: FileSystem = outPath.getFileSystem(conf)
//     outFs.delete(outPath, true)
//     outFs.mkdirs(outPath)

//     val sparkContext = args.sparkContext("Ingest")
//     try {
//       val rdd = sparkContext.hadoopGeoTiffRDD(inPath)
//       val (context, zoomLevel) = metadata(rdd, inPath, conf)
//       val tiles = tileRdd(rdd, zoomLevel, sparkContext)

//       val ingestSave = 
//         if(false) {
//           val tableName: String = ???
//           val layerName: String = ???

//           new AccumuloIngestSave(zoomLevel, tableName, layerName)
//         } else {
//           new MapFileIngestSave(context, zoomLevel, outPath, conf)
//         }

//       ingestSave.save(tiles)
//     } finally {
//       sparkContext.stop
//     }
//   }

//   def metadata(rdd: RDD[(Extent, Tile)], inPath: Path, conf: Configuration): (Context, ZoomLevel) = {
//     logInfo(s"Computing metadata from raster set...")
//     val (uncappedExtent, cellType, cellSize): (Extent, CellType, CellSize) =
//       rdd
//         .map { case (extent, tile) => (extent, tile.cellType, CellSize(extent, tile.cols, tile.rows)) }
//         .reduce { (t1, t2) =>
//         val (e1, ct1, cs1) = t1
//         val (e2, ct2, cs2) = t2
//         (e1.combine(e2), ct1.union(ct2),
//           if(cs1.resolution < cs2.resolution) cs1 else cs2
//         )
//       }

//     val tileScheme: TilingScheme = TilingScheme.GEODETIC
//     val zoomLevel: ZoomLevel = tileScheme.zoomLevelFor(cellSize)

//     val extent = tileScheme.extent.intersection(uncappedExtent).get

//     logInfo(s"Metadata: $extent, cellType = $cellType, cellSize = $cellSize")

//     val tileExtent = zoomLevel.tileExtentForExtent(extent)

//     val tileSizeBytes = TmsTiling.tileSizeBytes(zoomLevel.tileSize, cellType)
//     val blockSizeBytes = HdfsUtils.defaultBlockSize(inPath, conf)

//     val splitGenerator = RasterSplitGenerator(tileExtent, zoomLevel.level, tileSizeBytes, blockSizeBytes)
//     val partitioner = TileIdPartitioner(splitGenerator.splits)


//     val rasterMetadata =
//       RasterMetadata(TmsTiling.extentToPixel(extent, zoomLevel.level, zoomLevel.tileSize), tileExtent)

//     val meta: PyramidMetadata =
//       PyramidMetadata(
//         extent,
//         zoomLevel.tileSize,
//         1, // bands
//         Byte.MinValue.toDouble,
//         CellType.toAwtType(cellType),
//         zoomLevel.level,
//         Map(zoomLevel.level.toString -> rasterMetadata))

//     val context: Context =
//       Context(zoomLevel.level, meta, partitioner)

//     (context, zoomLevel)
//   }

//   // This is the mosaicing function. RDD[(Extent, Tile)] => RDD[TmsTile]
//   def tileRdd(rdd: RDD[(Extent, Tile)], zoomLevel: ZoomLevel, sc: SparkContext): RDD[TmsTile] = {
//     val bcZoomLevel = sc.broadcast(zoomLevel)
//     rdd
//       .flatMap { case (extent, tile) =>
//         val zoomLevel = bcZoomLevel.value
//         zoomLevel.tileIdsForExtent(extent).map { case tileId  => (tileId, (tileId, extent, tile)) }
//        }
//       .combineByKey( 
//         { case (tileId, extent, tile) =>
//           val zoomLevel = bcZoomLevel.value
//           val tmsTile = ArrayTile.empty(tile.cellType, zoomLevel.pixelCols, zoomLevel.pixelRows)
//           tmsTile.burnValues(zoomLevel.extentForTile(tileId), extent, tile)
//         },
//         { (tmsTile: MutableArrayTile, tup: (Long, Extent, Tile)) =>
//           val zoomLevel = bcZoomLevel.value
//           val (tileId, extent, tile) = tup
//           tmsTile.burnValues(zoomLevel.extentForTile(tileId), extent, tile)
//         },
//         { (tmsTile1: MutableArrayTile , tmsTile2: MutableArrayTile) =>
//           tmsTile1.burnValues(tmsTile2)
//         }
//        )
//       .map { case (id, tile) => TmsTile(id, tile) }
//   }
// }

// trait IngestSave extends Logging {
//   def save(tiles: RDD[TmsTile]): Unit
// }

// class AccumuloIngestSave(zoomLevel: ZoomLevel, tableName: String, layerName: String) extends IngestSave {
//   def save(tiles: RDD[TmsTile]): Unit = ???
// }

// class MapFileIngestSave(context: Context, zoomLevel: ZoomLevel, outPath: Path, conf: Configuration) extends IngestSave {
//   def save(tiles: RDD[TmsTile]): Unit = {
//     val metadata = context.toMetadata

//     val tileSizeBytes = TmsTiling.tileSizeBytes(zoomLevel.tileSize, metadata.cellType)
//     val blockSizeBytes = HdfsUtils.defaultBlockSize(outPath, conf)

//     val tileExtent = metadata.rasterMetadata.head._2.tileExtent

//     val splitGenerator = RasterSplitGenerator(tileExtent, zoomLevel.level, tileSizeBytes, blockSizeBytes)
//     val partitioner = TileIdPartitioner(splitGenerator.splits)
    
//     // Save pyramid metadata
//     val metaPath = new Path(outPath, PyramidMetadata.MetaFile)
//     val fs = metaPath.getFileSystem(conf)
//     val fdos = fs.create(metaPath)
//     val out = new PrintWriter(fdos)
//     out.println(JacksonWrapper.prettyPrint(metadata))
//     out.close()
//     fdos.close()

//     val outPathWithZoom = new Path(outPath, zoomLevel.level.toString)

//     tiles
//       .partitionBy(partitioner)
//       .withContext(context)
//       .saveAsHadoopRasterRDD(outPathWithZoom)

//     logInfo(s"Saved raster at zoom level ${zoomLevel.level} to $outPathWithZoom")

//   }
// }
