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
import geotrellis.raster.RasterData
import geotrellis.spark.Tile
import geotrellis.spark.cmd.args.HadoopArgs
import geotrellis.spark.cmd.args.SparkArgs
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.PayloadArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.formats.TileIdZoomWritable
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.metadata.RasterMetadata
import geotrellis.spark.rdd.MultiLevelTileIdPartitioner
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.rdd.RasterSplitGenerator
import geotrellis.spark.rdd.SplitGenerator
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.HdfsUtils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile
import org.apache.spark.Logging
import org.apache.spark.SerializableWritable
import org.apache.spark.SparkContext._
import org.apache.spark.SparkContext

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

class BuildPyramidArgs extends SparkArgs with HadoopArgs {
  @Required var pyramid: String = _
}

object BuildPyramid extends ArgMain[BuildPyramidArgs] with Logging {
  def main(args: BuildPyramidArgs) {
    val sc = args.sparkContext("BuildPyramid")
    val pyramid = new Path(args.pyramid)
    val conf = args.hadoopConf
    build(sc, pyramid, conf)
  }
  
  def build(sc: SparkContext, pyramid: Path, conf: Configuration) {
    // fill metadata with info on all higher levels
    val meta = fillMetadata(PyramidMetadata(pyramid, conf))

    // make splits for each higher level
    // TODO - revisit the "saving" of splits 
    // 1. Splits shouldn't be saved before the job is complete - needs refactoring of TileIdPartitioner's apply that takes gen
    // 2. The base zoom level shouldn't be rewritten. This will be made easier once #1 is done. 
    // Addendum for #2: base zoom is no longer part of mp, so #2 can be ignored
    val splits = getSplits(meta, HdfsUtils.defaultBlockSize(pyramid, conf))
    val mp = MultiLevelTileIdPartitioner(splits, pyramid, conf)
    logInfo(s"${mp}")

    val rasterPath = new Path(pyramid, meta.maxZoomLevel.toString)
    val rdd = RasterRDD(rasterPath, sc)

    // broadcast configuration and metadata
    val broadCastedConf = sc.broadcast(new SerializableWritable(conf))
    val broadCastedMeta = sc.broadcast(meta)
    val pyramidStr = pyramid.toUri().toString()

    val res = rdd
      .mapPartitions({ partition =>
        val meta = broadCastedMeta.value
        partition.map(MultiLevelWarper.warp(_, meta)).flatten
      }, true)
      .groupByKey(mp)
      .mapPartitions({ partition =>
        val meta = broadCastedMeta.value
        val conf = broadCastedConf.value.value

        // if partition is empty don't create a part file
        if (!partition.isEmpty) {
          val buf = partition.toArray.sortWith((x, y) => x._1.get() < y._1.get())
          val (zoom, index) = (buf.head._1.zoom, mp.getPartitionForZoom(buf.head._1))
          val outPath = new Path(pyramidStr, zoom.toString)
          val mapFilePath = new Path(outPath, f"part-${index}%05d")
          val fs = mapFilePath.getFileSystem(conf)
          conf.set("io.map.index.interval", "1")
          logInfo(s"Working on partition ${index}")
          val writer = new MapFile.Writer(conf, fs, mapFilePath.toUri.toString,
            classOf[TileIdWritable], classOf[ArgWritable], SequenceFile.CompressionType.RECORD)

          buf.foreach { tiles =>
            val (tw, aw) = Stitcher.stitch(tiles, meta)
            writer.append(tw, aw)
          }
          writer.close()
        }
        partition
      }, true)

    meta.save(pyramid, conf)
    logInfo(s"Result has ${res.count} tuples")
  }

  private def fillMetadata(meta: PyramidMetadata): PyramidMetadata = {
    val newRasterMetadata = meta.rasterMetadata ++
      ((1 until meta.maxZoomLevel) map { zoom =>
        val tileExtent = TmsTiling.extentToTile(meta.extent, zoom, meta.tileSize)
        val pixelExtent = TmsTiling.extentToPixel(meta.extent, zoom, meta.tileSize)
        (zoom.toString -> RasterMetadata(pixelExtent, tileExtent))
      }).toMap

    val newMeta = meta.copy(rasterMetadata = newRasterMetadata)
    newMeta
  }

  private def getSplits(meta: PyramidMetadata, blockSize: Long): Map[Int, SplitGenerator] = {
    logInfo("Creating RasterSplitGenerator, using blocksize = " + blockSize)
    val tileSize = TmsTiling.tileSizeBytes(meta.tileSize, meta.rasterType)
    for ((zoom, rm) <- meta.rasterMetadata if zoom.toInt < meta.maxZoomLevel)
      yield (zoom.toInt -> RasterSplitGenerator(rm.tileExtent, zoom.toInt, tileSize, blockSize))
  }

  private def tileSizeForZoomLevel(tileSize: Int, maxZoom: Int, zoom: Int): Int =
    math.max(1, (tileSize / math.pow(2, maxZoom - zoom)).toInt)

  // this object encapsulates the actual warping work of warping a single tile into multiple tiles, one for each zoom levels 
  private object MultiLevelWarper {
    // warp the input tile to each of the higher pyramid levels
    def warp(tile: Tile, meta: PyramidMetadata): Seq[(TileIdZoomWritable, PayloadArgWritable)] = {
      // Nomenclature -           
      // par or parent is the input tile, typically from base zoom, 
      // ch or child is the tile being warped into, which is one for each zoom level, and differing sizes 
      // prev is the tile from the "previous" zoom level. Since each input tile is warped into one tile  
      // at each higher zoom level, "previous" is always a unique tile
      val (maxZoom, tileSize) = (meta.maxZoomLevel, meta.tileSize)

      val parTzw = TileIdZoomWritable(tile.id, maxZoom)
      val parRd = tile.raster.data
      val parExtent = tile.raster.rasterExtent
      val parExtent2 = TmsTiling.tileToExtent(tile.id, maxZoom, tileSize)
      assert(parExtent.extent == parExtent2)

      // initialize the map with info on the input tile, we'll remove it later before returning from warp 
      val initCond = collection.mutable.Map[Int, (TileIdZoomWritable, RasterData, RasterExtent)](maxZoom -> (parTzw, parRd, parExtent))
      val tileMap = (meta.maxZoomLevel - 1 to 1 by -1).foldLeft(initCond) { (m, zoom) =>
        val chTs = tileSizeForZoomLevel(tileSize, maxZoom, zoom)
        val chTxTy = TmsTiling.latLonToTile(parExtent.extent.ymin, parExtent.extent.xmin, zoom, tileSize)
        val chTileId = TmsTiling.tileId(chTxTy.tx, chTxTy.ty, zoom)

        val prev = m(zoom + 1)
        val (prevRd, prevExtent) = (prev._2, prev._3)
        val chExtent = RasterExtent(prevExtent.extent, chTs, chTs)
        val chRd = prevRd.warp(prevExtent, chExtent)
        //println(s"Mapper: z=${zoom}, tid=${chTileId} parTid=${prev._1.get()}, chTs=${chTs}")
        m += (zoom -> (TileIdZoomWritable(chTileId, zoom), chRd, chExtent))
      }
      tileMap -= maxZoom
      tileMap.values.toSeq.map(t => (t._1, PayloadArgWritable.fromPayloadRasterData(t._2, parTzw)))
    }
  }

  // this object encapsulates the stitching of multiple tiles from one zoom level onto a single tile of a higher zoom level 
  private object Stitcher {
    def stitch(tiles: (TileIdZoomWritable, Seq[PayloadArgWritable]), meta: PyramidMetadata): (TileIdWritable, ArgWritable) = {
      // Nomenclature -           
      // ch or child is the tile being written to, so 512x512, 
      // par or parent is the tile from base zoom, and
      // chPt or "part of child" is the tile being pasted onto child, and is called as such
      // since there can be in general more than one tiles being pasted onto child

      val (chZoom, chTileId) = (tiles._1.zoom, tiles._1.get())
      val chExtent = TmsTiling.tileToExtent(chTileId, chZoom, meta.tileSize)
      val chCorner = TmsTiling.latLonToPixelsUL(chExtent.ymax, chExtent.xmin, chZoom, meta.tileSize)

      val parTzw = new TileIdZoomWritable()
      val chRd = RasterData.emptyByType(meta.rasterType, meta.tileSize, meta.tileSize)
      val chPtTs = tileSizeForZoomLevel(meta.tileSize, meta.maxZoomLevel, chZoom)

      for (paw <- tiles._2) {
        // get the raster
        val chPtRd = paw.toPayloadRasterData(meta.rasterType, chPtTs, chPtTs, parTzw)
        assert(chPtRd.cols == chPtTs && chPtRd.rows == chPtTs)

        // get the location into child raster to start painting into
        val parExtent = TmsTiling.tileToExtent(parTzw.get(), parTzw.zoom, meta.tileSize)
        val chStart = TmsTiling.latLonToPixelsUL(parExtent.ymax, parExtent.xmin, chZoom, meta.tileSize)
        val (chPx, chPy) = ((chStart.px - chCorner.px).toInt, (chStart.py - chCorner.py).toInt)

        //println(s"Reducer: z=${chZoom}, tid=${chTileId}, parTid=${parTzw.get()}, chPx=${chPx}, chPy=${chPy}, chPtTs=${chPtTs} isFloat=${chPtRd.isFloat}")

        // TODO - use spire
        for (py <- 0 until chPtTs) {
          for (px <- 0 until chPtTs) {

            // occasionally we may get chPx,chPy wrong (needs to be investigated why)
            // this guards the ArrayOutOfBoundsException
            val x = chPx + px
            val y = chPy + py
            if (x < meta.tileSize && y < meta.tileSize) {
              if (!chPtRd.isFloat) {
                chRd.set(chPx + px, chPy + py, chPtRd.get(px, py))
              } else {
                chRd.setDouble(chPx + px, chPy + py, chPtRd.getDouble(px, py))
              }
            }
          }
        }
      }
      (TileIdWritable(chTileId), ArgWritable.fromRasterData(chRd))
    }
  }
}