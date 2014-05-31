/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.raster

import geotrellis._
import geotrellis.feature.Extent

import scalaxy.loops._
import scala.collection.mutable

//object CroppedRaster {
  // def apply(sourceRaster: Raster, gridBounds: GridBounds): CroppedRaster = 
  //   CroppedRaster(sourceRaster, gridBounds)

  // def apply(sourceRaster: Raster, extent: Extent): CroppedRaster =
  //   CroppedRaster(sourceRaster, sourceRaster.rasterExtent.gridBoundsFor(extent), extent)
//}

case class CroppedTile(sourceTile: Tile,
                         gridBounds: GridBounds) 
  extends Tile {
  val cols = gridBounds.width
  val rows = gridBounds.height
  // val rasterExtent = RasterExtent(extent,
  //                                 sourceRaster.rasterExtent.cellwidth,
  //                                 sourceRaster.rasterExtent.cellheight,
  //                                 gridBounds.width,
  //                                 gridBounds.height)
  def force = toArrayTile
  val rasterType = sourceTile.rasterType

  private val colMin = gridBounds.colMin
  private val rowMin = gridBounds.rowMin
  private val sourceCols = sourceTile.cols
  private val sourceRows = sourceTile.rows

  def warp(source: Extent, target: RasterExtent) = 
    toArrayTile.warp(source, target)

  def get(col: Int, row: Int): Int = {
    val c = col + gridBounds.colMin
    val r = row + gridBounds.rowMin
    if(c < 0 || r < 0 || c >= sourceCols || r >= sourceRows) {
      NODATA
    } else {
      sourceTile.get(c,r)
    }
  }

  def getDouble(col: Int, row: Int): Double = {
    val c = col + gridBounds.colMin
    val r = row + gridBounds.rowMin

    if(c < 0 || r < 0 || c >= sourceCols || r >= sourceRows) {
      Double.NaN
    } else {
      sourceTile.getDouble(col+gridBounds.colMin,row+gridBounds.rowMin)
    }
  }

  def toArrayTile: ArrayTile = {
    val data = ArrayTile.allocByType(rasterType,cols,rows)
    if(!isFloat) {
      for(row <- 0 until rows optimized) {
        for(col <- 0 until cols optimized) {
          data.set(col, row, get(col,row))
        }
      }
    } else {
      for(row <- 0 until rows optimized) {
        for(col <- 0 until cols optimized) {
          data.setDouble(col, row, getDouble(col,row))
        }
      }
    }
    ArrayTile(data, cols, rows)
  }

  def toArray: Array[Int] = {
    val arr = Array.ofDim[Int](cols * rows)
    var i = 0
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        arr(i) = get(col,row)
        i += 1
      }
    }
    arr
  }

  def toArrayDouble: Array[Double] = {
    val arr = Array.ofDim[Double](cols * rows)
    var i = 0
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        arr(i) = getDouble(col,row)
        i += 1
      }
    }
    arr
  }

  def toArrayByte(): Array[Byte] = toArrayTile.toArrayByte

  def data: ArrayTile = toArrayTile.data

  def copy() = 
    if(isFloat) {
      Tile(toArray, cols, rows) 
    } else {
      Tile(toArrayDouble, cols, rows)
    }

  def convert(typ: RasterType): Tile = 
    sourceTile.convert(typ)

  def map(f: Int => Int): Tile = {
    val data = ArrayTile.allocByType(rasterType, cols, rows)
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        data.set(col,row, get(col,row))
      }
    }
    ArrayTile(data, cols, rows)
  }

  def combine(r2: Tile)(f: (Int, Int) => Int): Tile = {
    if(this.dimensions != r2.dimensions) {
      throw new GeoAttrsError("Cannot combine rasters with different dimensions." +
                             s"$dimensions does not match ${r2.dimensions}")
    }
    val data = ArrayTile.allocByType(rasterType, cols, rows)
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        data.set(col, row, f(get(col, row), r2.get(col, row)))
      }
    }
    Tile(data, cols, rows)
  }

  def mapDouble(f: Double =>Double): Tile = {
    val data = ArrayTile.allocByType(rasterType, cols, rows)
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        data.setDouble(col, row, getDouble(col,row))
      }
    }
    ArrayTile(data, cols, rows)
  }

  def combineDouble(r2: Tile)(f: (Double, Double) => Double): Tile = {
    if(this.dimensions != r2.dimensions) {
      throw new GeoAttrsError("Cannot combine rasters with different dimensions." +
                             s"$dimensions does not match ${r2.dimensions}")
    }
    val data = ArrayTile.allocByType(rasterType, cols, rows)
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        data.setDouble(col, row, f(getDouble(col, row), r2.getDouble(col, row)))
      }
    }
    Tile(data, cols, rows)
  }
}
