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

package geotrellis.spark.ingest

import geotrellis.raster._
import geotrellis.feature.Extent
import geotrellis.spark.cmd.NoDataHandler
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.tiling.TmsTiling

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.InputSplit
import org.apache.hadoop.mapreduce.JobContext
import org.apache.hadoop.mapreduce.RecordReader
import org.apache.hadoop.mapreduce.TaskAttemptContext
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.input.FileSplit
import org.geotools.coverage.grid.GridCoverage2D

import java.awt.image.DataBufferByte
import java.awt.image.DataBufferDouble
import java.awt.image.DataBufferFloat
import java.awt.image.DataBufferInt
import java.awt.image.DataBufferShort

object TiffTiler {
  
  /*
   * Given a path to a tiff, this produces a set of tiles for that tiff. Uses CroppedRaster 
   * from core geotrellis to produce the actual tiles  
   */
  def tile(file: Path, pyMeta: PyramidMetadata, conf: Configuration): List[(Long, Tile)] = {
    val (raster, extent) =
      GeoTiff.withReader[(Tile, Extent)](file, conf) { reader =>
        val image = GeoTiff.getGridCoverage2D(reader)
        val imageMeta = GeoTiff.getMetadata(reader)
        (warp(image, imageMeta, pyMeta), imageMeta.extent)
      }

    val (zoom, tileSize, cellType, nodata) =
      (pyMeta.maxZoomLevel, pyMeta.tileSize, pyMeta.cellType, pyMeta.nodata)

    val tileExtent = TmsTiling.extentToTile(extent, zoom, tileSize)

    val tiles = for {
      ty <- tileExtent.ymin to tileExtent.ymax
      tx <- tileExtent.xmin to tileExtent.xmax
      tileId = TmsTiling.tileId(tx, ty, zoom)
      // can the third argument just be tileExtent?
      cropTileTmp = CroppedTile(raster, extent, TmsTiling.tileToExtent(tx, ty, zoom, tileSize))
      // TODO - do away with the second crop. It is put in as workaround for the case when 
      // CroppedRaster's translation from Extent to gridBounds ends up giving us an extra row/col
      cropRaster = CroppedTile(cropTileTmp, GridBounds(0, 0, tileSize - 1, tileSize - 1))
    } yield (tileId, cropRaster)

    tiles.toList
  }

  private def warp(image: GridCoverage2D, imgMeta: GeoTiff.Metadata, pyMeta: PyramidMetadata): Tile = {
    val (pixelWidth, pixelHeight) = imgMeta.pixels

    val (zoom, tileSize, cellType, nodata) =
      (pyMeta.maxZoomLevel, pyMeta.tileSize, pyMeta.cellType, pyMeta.nodata)

    val origRaster = {
      val rawDataBuff = image.getRenderedImage().getData().getDataBuffer()
      val tile = cellType match {
        case TypeDouble => ArrayTile(rawDataBuff.asInstanceOf[DataBufferDouble].getData(), tileSize, tileSize)
        case TypeFloat  => ArrayTile(rawDataBuff.asInstanceOf[DataBufferFloat].getData(), tileSize, tileSize)
        case TypeInt    => ArrayTile(rawDataBuff.asInstanceOf[DataBufferInt].getData(), tileSize, tileSize)
        case TypeShort  => ArrayTile(rawDataBuff.asInstanceOf[DataBufferShort].getData(), tileSize, tileSize)
        case TypeByte   => ArrayTile(rawDataBuff.asInstanceOf[DataBufferByte].getData(), tileSize, tileSize)
        case _          => sys.error("Unrecognized AWT type - " + cellType)
      }
      NoDataHandler.removeUserNoData(tile, nodata)
      tile
    }

    val res = TmsTiling.resolution(zoom, tileSize)
    val newRe = RasterExtent(imgMeta.extent, res, res)
    //println(s"re: ${re},newRe: ${newRe}")
    //val start = System.currentTimeMillis
    //println(s"[extent,cols,rows,cellwidth,cellheight,rdLen,bufLen]: ")
    //println(s"Before Warp: [${r.rasterExtent},${r.cols},${r.rows},${r.rasterExtent.cellwidth},${r.rasterExtent.cellheight},${r.toArrayRaster.data.length},${rawDataBuff.asInstanceOf[DataBufferFloat].getData().length}]")
    val warpRaster = origRaster.warp(imgMeta.extent, newRe)
    //val end = System.currentTimeMillis
    //println(s"After Warp: [${wr.rasterExtent},${wr.cols},${wr.rows},${wr.rasterExtent.cellwidth},cellheight=${wr.rasterExtent.cellheight},${wr.toArrayRaster.data.length}]")
    //println(s"Warp operation took ${end - start} ms.")
    warpRaster
  }
}
