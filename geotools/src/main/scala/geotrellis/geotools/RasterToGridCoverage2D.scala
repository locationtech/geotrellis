/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.geotools

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.vector._

import org.geotools.coverage.grid._
import org.geotools.coverage.grid.io._
import org.geotools.gce.geotiff._
import org.geotools.geometry.Envelope2D
import org.geotools.referencing.{CRS => GeoToolsCRS}
import org.geotools.referencing.operation.transform._
import org.osgeo.proj4j._

import java.awt.image.{DataBuffer, SampleModel}
import javax.media.jai.{RasterFactory, TiledImage}


trait RasterToGridCoverage2D {

  def dataType(cellType: CellType) = {
    cellType.name match {
      case "int8" => DataBuffer.TYPE_BYTE
      case "uint8" => DataBuffer.TYPE_BYTE
      case "uint16" => DataBuffer.TYPE_USHORT
      case "int16" => DataBuffer.TYPE_SHORT
      case "int32" => DataBuffer.TYPE_INT
      case "float32" => DataBuffer.TYPE_FLOAT
      case "float64" => DataBuffer.TYPE_DOUBLE
      case _ => throw new Exception("Unknown or Unrepresentable CellType")
    }
  }

  def sampleModel(tile: Tile): SampleModel = {
    val dataType = this.dataType(tile.cellType)
    RasterFactory.createBandedSampleModel(dataType, tile.cols, tile.rows, 1)
  }

  def sampleModel(tile: MultibandTile): SampleModel = {
    val dataType = this.dataType(tile.cellType)
    RasterFactory.createBandedSampleModel(dataType, tile.cols, tile.rows, tile.bandCount)
  }

  def planarImage(tile: Tile): TiledImage = {
    val sampleModel = this.sampleModel(tile)
    val tiledImage = new TiledImage(0, 0, tile.cols, tile.rows, 0, 0, sampleModel, null)

    var row = 0; while (row < tile.rows) {
      var col = 0; while(col < tile.cols) {
        if (tile.cellType.isFloatingPoint)
          tiledImage.setSample(col, row, 0, tile.getDouble(col, row))
        else
          tiledImage.setSample(col, row, 0, tile.get(col, row))
        col += 1
      }
      row += 1
    }

    tiledImage
  }

  def planarImage(tile: MultibandTile): TiledImage = {
    val sampleModel = this.sampleModel(tile)
    val tiledImage = new TiledImage(0, 0, tile.cols, tile.rows, 0, 0, sampleModel, null)

    var b = 0; while (b < tile.bandCount) {
      var row = 0; while (row < tile.rows) {
        var col = 0; while(col < tile.cols) {
          if (tile.cellType.isFloatingPoint)
            tiledImage.setSample(col, row, b, tile.band(b).getDouble(col, row))
          else
            tiledImage.setSample(col, row, b, tile.band(b).get(col, row))
          col += 1
        }
        row += 1
      }
      b += 1
    }

    tiledImage
  }

  def gridGeometry2D[T <: CellGrid](raster: Raster[T], crs: Option[CRS]): GridGeometry2D = {
    val Raster(tile, Extent(xmin, ymin, xmax, ymax)) = raster
    val grid: org.opengis.coverage.grid.GridEnvelope = new GridEnvelope2D(0, 0, tile.cols, tile.rows)
    val geoToolsCRS = crs match {
      case Some(crs) =>
        GeoToolsCRS.decode(s"EPSG:${crs.epsgCode.get}")
      case None =>
        GeoToolsCRS.decode("EPSG:404000")
    }
    val envelope: org.opengis.geometry.Envelope = new Envelope2D(geoToolsCRS, xmin, ymin, (xmax - xmin), (ymax - ymin))

    new GridGeometry2D(grid, envelope)
  }

}

object TileRasterToGridCoverage2D extends RasterToGridCoverage2D {

  def apply(raster: Raster[Tile], crs: Option[CRS]) = {
    val name = raster.toString
    val image = planarImage(raster.tile)
    val gridGeometry = gridGeometry2D(raster, crs)
    val factory = new GridCoverageFactory

    factory.create(name, image, gridGeometry, null, null, null)
  }
}

object MultibandTileRasterToGridCoverage2D extends RasterToGridCoverage2D {

  def apply(raster: Raster[MultibandTile], crs: Option[CRS]) = {
    val name = raster.toString
    val image = planarImage(raster.tile)
    val gridGeometry = gridGeometry2D(raster, crs)
    val factory = new GridCoverageFactory

    factory.create(name, image, gridGeometry, null, null, null)
  }
}
