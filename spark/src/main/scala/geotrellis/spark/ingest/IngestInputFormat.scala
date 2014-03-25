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

import geotrellis._
import geotrellis.raster.RasterData
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

class IngestInputFormat extends FileInputFormat[Long, Raster] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[Long, Raster] =
    new IngestRecordReader

}

class IngestRecordReader extends RecordReader[Long, Raster] {
  private var tiles: List[(Long, Raster)] = null
  private var index: Int = -1

  def initialize(split: InputSplit, context: TaskAttemptContext) = {
    val file = split.asInstanceOf[FileSplit].getPath()
    val meta = PyramidMetadata.readFromJobConf(context.getConfiguration())
    tiles = tiffToTiles(file, meta, context.getConfiguration())
  }

  def close = {}
  def getCurrentKey = tiles(index)._1
  def getCurrentValue = tiles(index)._2
  def getProgress = (index + 1) / tiles.length
  def nextKeyValue = {
    index = index + 1
    index < tiles.length
  }

  /* Note that this tiffToTiles is slightly different from Ingest.tiffToTiles */
  private def tiffToTiles(file: Path, pyMeta: PyramidMetadata, conf: Configuration): List[(Long, Raster)] = {
    val raster = GeoTiff.withReader(file, conf) { reader =>
      {
        val image = GeoTiff.getGridCoverage2D(reader)
        val imageMeta = GeoTiff.getMetadata(reader)
        warp(image, imageMeta, pyMeta)
      }
    }
    val (zoom, tileSize, rasterType, nodata) =
      (pyMeta.maxZoomLevel, pyMeta.tileSize, pyMeta.rasterType, pyMeta.nodata)
    val extent = raster.rasterExtent.extent
    val tileExtent = TmsTiling.extentToTile(extent, zoom, tileSize)

    val tiles = for {
      ty <- tileExtent.ymin to tileExtent.ymax
      tx <- tileExtent.xmin to tileExtent.xmax
      tileId = TmsTiling.tileId(tx, ty, zoom)
      extent = TmsTiling.tileToExtent(tx, ty, zoom, tileSize)
      cropRasterTmp = CroppedRaster(raster, extent)
      // TODO - do away with the second crop. It is put in as workaround for the case when 
      // CroppedRaster's translation from Extent to gridBounds ends up giving us an extra row/col
      cropRaster = CroppedRaster(cropRasterTmp, GridBounds(0, 0, tileSize - 1, tileSize - 1))
    } yield (tileId, cropRaster)

    tiles.toList
  }

  private def warp(image: GridCoverage2D, imgMeta: GeoTiff.Metadata, pyMeta: PyramidMetadata): Raster = {

    val (pixelWidth, pixelHeight) = imgMeta.pixels

    val (zoom, tileSize, rasterType, nodata) =
      (pyMeta.maxZoomLevel, pyMeta.tileSize, pyMeta.rasterType, pyMeta.nodata)

    def buildRaster = {
      val extent = Extent(imgMeta.bounds.getLowerCorner.getOrdinate(0),
        imgMeta.bounds.getLowerCorner.getOrdinate(1),
        imgMeta.bounds.getUpperCorner.getOrdinate(0),
        imgMeta.bounds.getUpperCorner.getOrdinate(1))
      val re = RasterExtent(extent, pixelWidth, pixelHeight)
      val rawDataBuff = image.getRenderedImage().getData().getDataBuffer()
      val rd = rasterType match {
        case TypeDouble => RasterData(rawDataBuff.asInstanceOf[DataBufferDouble].getData(), tileSize, tileSize)
        case TypeFloat  => RasterData(rawDataBuff.asInstanceOf[DataBufferFloat].getData(), tileSize, tileSize)
        case TypeInt    => RasterData(rawDataBuff.asInstanceOf[DataBufferInt].getData(), tileSize, tileSize)
        case TypeShort  => RasterData(rawDataBuff.asInstanceOf[DataBufferShort].getData(), tileSize, tileSize)
        case TypeByte   => RasterData(rawDataBuff.asInstanceOf[DataBufferByte].getData(), tileSize, tileSize)
        case _          => sys.error("Unrecognized AWT type - " + rasterType)
      }
      val trd = NoDataHandler.removeUserNoData(rd, nodata)
      Raster(trd, re)
    }

    val origRaster = buildRaster
    val res = TmsTiling.resolution(zoom, tileSize)
    val newRe = RasterExtent(origRaster.rasterExtent.extent, res, res)
    //println(s"re: ${re},newRe: ${newRe}")
    //val start = System.currentTimeMillis
    //println(s"[extent,cols,rows,cellwidth,cellheight,rdLen,bufLen]: ")
    //println(s"Before Warp: [${r.rasterExtent},${r.cols},${r.rows},${r.rasterExtent.cellwidth},${r.rasterExtent.cellheight},${r.toArrayRaster.data.length},${rawDataBuff.asInstanceOf[DataBufferFloat].getData().length}]")
    val warpRaster = origRaster.warp(newRe)
    //val end = System.currentTimeMillis
    //println(s"After Warp: [${wr.rasterExtent},${wr.cols},${wr.rows},${wr.rasterExtent.cellwidth},cellheight=${wr.rasterExtent.cellheight},${wr.toArrayRaster.data.length}]")
    //println(s"Warp operation took ${end - start} ms.")
    warpRaster
  }
}