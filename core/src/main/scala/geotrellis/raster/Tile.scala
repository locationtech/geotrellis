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

import geotrellis.feature.Extent

import scalaxy.loops._

object Tile {
  def apply(arr: Array[Int], cols: Int, rows: Int): Tile = 
    IntArrayTile(arr, cols, rows)

  def apply(arr: Array[Double], cols: Int, rows: Int): Tile = 
    DoubleArrayTile(arr, cols, rows)

  def empty(cols: Int, rows: Int): Tile = 
    IntArrayTile.empty(cols, rows)
}

/**
 * Base trait for the Tile data type.
 */
trait Tile extends Raster /*with local.LocalMethods*/ {
  type This = Tile

  val cols: Int
  val rows: Int
  lazy val dimensions: (Int, Int) = (cols, rows)
  lazy val length = cols * rows

  val cellType: CellType

  /**
   * Get value at given coordinates.
   */
  def get(col: Int, row: Int): Int

  /**
   * Get value at given coordinates.
   */
  def getDouble(col: Int, row: Int): Double

  def toArrayTile(): ArrayTile
  def toArray(): Array[Int]
  def toArrayDouble(): Array[Double]
  def toBytes(): Array[Byte]

  def foreach(f: Int=>Unit): Unit =
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        f(get(col, row))
      }
    }

  def foreachDouble(f: Double=>Unit): Unit =
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        f(getDouble(col, row))
      }
    }

  def map(f: Int => Int): Tile
  def combine(r2: Tile)(f: (Int, Int) => Int): Tile

  def mapDouble(f: Double => Double): Tile
  def combineDouble(r2: Tile)(f: (Double, Double) => Double): Tile

  /**
   * Normalizes the values of this raster, given the current min and max, to a new min and max.
   * 
   *   @param oldMin    Old mininum value
   *   @param oldMax    Old maximum value
   *   @param newMin     New minimum value
   *   @param newMax     New maximum value
   */
  def normalize(oldMin: Int, oldMax: Int, newMin: Int, newMax: Int): Tile = {
    val dnew = newMax - newMin
    val dold = oldMax - oldMin
    if(dold <= 0 || dnew <= 0) { sys.error(s"Invalid parameters: $oldMin, $oldMax, $newMin, $newMax") }
    mapIfSet(z => ( ((z - oldMin) * dnew) / dold ) + newMin)
  }

  def warp(source: Extent, target: RasterExtent): Tile

  def warp(source: Extent, target: Extent): Tile =
    warp(source, RasterExtent(source, cols, rows).createAligned(target))

  def warp(source: Extent, targetCols: Int, targetRows: Int): Tile =
    warp(source, RasterExtent(source, targetCols, targetRows))

  /**
   * Return tuple of highest and lowest value in raster.
   *
   * @note   Currently does not support double valued raster data types
   *         (TypeFloat, TypeDouble). Calling findMinMax on rasters of those
   *         types will give the integer min and max of the rounded values of
   *         their cells.
   */
  def findMinMax = {
    var zmin = Int.MaxValue
    var zmax = Int.MinValue

    foreach { 
      z => if (isData(z)) {
        zmin = math.min(zmin, z)
        zmax = math.max(zmax, z)
      }
    }

    if(zmin == Int.MaxValue) { zmin = NODATA }
    (zmin, zmax)
  } 

  /**
   * Return tuple of highest and lowest value in raster.
   */
  def findMinMaxDouble = {
    var zmin = Double.NaN
    var zmax = Double.NaN

    foreachDouble {
      z => if (isData(z)) {
        if(isNoData(zmin)) {
          zmin = z
          zmax = z
        } else {
          zmin = math.min(zmin, z)
          zmax = math.max(zmax, z)
        }
      }
    }

    (zmin, zmax)
  }

  /**
   * Return ascii art of this raster.
   */
  def asciiDraw(): String = { 
    val sb = new StringBuilder
    for(row <- 0 until rows) {
      for(col <- 0 until cols) {
        val v = get(col, row)
        val s = if(isNoData(v)) {
          "ND"
        } else {
          s"$v"
        }
        val pad = " " * math.max(6 - s.length, 0) 
        sb.append(s"$pad$s")
      }
      sb += '\n'
    }      
    sb += '\n'
    sb.toString
  }

  /**
   * Return ascii art of a range from this raster.
   */
  def asciiDrawRange(colMin: Int, colMax: Int, rowMin: Int, rowMax: Int) = {
    var s = "";
    for (row <- rowMin to rowMax) {
      for (col <- colMin to colMax) {
        val z = this.get(row, col)
        if (isNoData(z)) {
          s += ".."
        } else {
          s += "%02X".format(z)
        }
      }
      s += "\n"
    }
    s
  }
}
