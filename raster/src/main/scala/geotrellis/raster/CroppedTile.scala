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

import geotrellis.raster.interpolation._
import geotrellis.vector.Extent

import spire.syntax.cfor._
import scala.collection.mutable

object CroppedTile {
  def apply(sourceTile: Tile,
            sourceExtent: Extent,
            targetExtent: Extent): CroppedTile =
    CroppedTile(
      sourceTile,
      RasterExtent(
        sourceExtent,
        sourceTile.cols, 
        sourceTile.rows
      ).gridBoundsFor(targetExtent)
    )
}

case class CroppedTile(sourceTile: Tile,
                       gridBounds: GridBounds)
  extends Tile {
  val cols = gridBounds.width
  val rows = gridBounds.height

  val cellType = sourceTile.cellType

  private val colMin = gridBounds.colMin
  private val rowMin = gridBounds.rowMin
  private val sourceCols = sourceTile.cols
  private val sourceRows = sourceTile.rows

  def get(col: Int, row: Int): Int = {
    val c = col + gridBounds.colMin
    val r = row + gridBounds.rowMin
    if(c < 0 || r < 0 || c >= sourceCols || r >= sourceRows) {
      NODATA
    } else {
      sourceTile.get(c, r)
    }
  }

  def getDouble(col: Int, row: Int): Double = {
    val c = col + gridBounds.colMin
    val r = row + gridBounds.rowMin

    if(c < 0 || r < 0 || c >= sourceCols || r >= sourceRows) {
      Double.NaN
    } else {
      sourceTile.getDouble(col + gridBounds.colMin, row + gridBounds.rowMin)
    }
  }

  def toArrayTile: ArrayTile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)

    if(!cellType.isFloatingPoint) {
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          tile.set(col, row, get(col, row))
        }
      }
    } else {
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          tile.setDouble(col, row, getDouble(col, row))
        }
      }
    }

    tile
  }

  def toArray: Array[Int] = {
    val arr = Array.ofDim[Int](cols * rows)

    var i = 0
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        arr(i) = get(col, row)
        i += 1
      }
    }

    arr
  }

  def toArrayDouble: Array[Double] = {
    val arr = Array.ofDim[Double](cols * rows)

    var i = 0
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        arr(i) = getDouble(col, row)
        i += 1
      }
    }

    arr
  }

  def toBytes(): Array[Byte] = toArrayTile.toBytes

  def map(f: Int => Int): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.set(col, row, f(get(col, row)))
      }
    }

    tile
  }

  def combine(other: Tile)(f: (Int, Int) => Int): Tile = {
    (this, other).assertEqualDimensions

    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.set(col, row, f(get(col, row), other.get(col, row)))
      }
    }

    tile
  }

  def mapDouble(f: Double =>Double): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.setDouble(col, row, f(getDouble(col, row)))
      }
    }

    tile
  }

  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = {
    (this, other).assertEqualDimensions

    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.setDouble(col, row, f(getDouble(col, row), other.getDouble(col, row)))
      }
    }

    tile
  }

  def resample(source: Extent, target: RasterExtent, method: InterpolationMethod): Tile = 
    toArrayTile.resample(source, target, method)
}
