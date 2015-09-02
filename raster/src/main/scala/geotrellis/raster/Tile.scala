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

import geotrellis.vector.Extent
import geotrellis.raster.op.stats._

import spire.syntax.cfor._

import java.util.Locale

import math.BigDecimal

import collection.mutable.ArrayBuffer

/**
  * Base trait for a Tile.
  */
trait Tile extends CellGrid with IterableTile with MappableTile[Tile] {

  def dualForeach(f: Int => Unit)(g: Double => Unit): Unit =
    if (cellType.isFloatingPoint) foreachDouble(g) else foreach(f)

  def mapIfSet(f: Int => Int): Tile =
    map { i =>
      if(isNoData(i)) i
      else f(i)
    }

  def mapIfSetDouble(f: Double => Double): Tile =
    mapDouble { d =>
      if(isNoData(d)) d
      else f(d)
    }

  def dualMap(f: Int => Int)(g: Double => Double): Tile =
    if (cellType.isFloatingPoint) mapDouble(g) else map(f)

  def dualMapIfSet(f: Int => Int)(g: Double => Double): Tile =
    if (cellType.isFloatingPoint) mapIfSetDouble(g) else mapIfSet(f)

  def dualCombine(r2: Tile)(f: (Int, Int) => Int)(g: (Double, Double) => Double): Tile =
    if (cellType.union(r2.cellType).isFloatingPoint) combineDouble(r2)(g) else combine(r2)(f)

  /** Create a mutable copy of this tile */
  def mutable: MutableArrayTile 

  def convert(cellType: CellType): Tile

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

  def foreach(f: Int=>Unit): Unit

  def foreachDouble(f: Double=>Unit): Unit 

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

  /**
    * Normalizes the values of this raster, given the current min and max, to a new min and max.
    *
    *   @param oldMin    Old mininum value
    *   @param oldMax    Old maximum value
    *   @param newMin     New minimum value
    *   @param newMax     New maximum value
    */
  def normalize(oldMin: Double, oldMax: Double, newMin: Double, newMax: Double): Tile = {
    val dnew = newMax - newMin
    val dold = oldMax - oldMin
    if(dold <= 0 || dnew <= 0) { sys.error(s"Invalid parameters: $oldMin, $oldMax, $newMin, $newMax") }
    mapIfSetDouble(z => ( ((z - oldMin) * dnew) / dold ) + newMin)
  }

  def rescale(newMin: Int, newMax: Int) = {
    val (min, max) = findMinMax
    normalize(min, max, newMin, newMax)
  }

  def rescale(newMin: Double, newMax: Double) = {
    val (min, max) = findMinMaxDouble
    normalize(min, max, newMin, newMax)
  }

  def crop(cols: Int, rows: Int): Tile =
    CroppedTile(this, GridBounds(0, 0, cols - 1, rows - 1))

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): Tile =
    CroppedTile(this, GridBounds(colMin, rowMin, colMax, rowMax))

  def crop(gb: GridBounds): Tile =
    CroppedTile(this, gb)

  def crop(srcExtent: Extent, extent: Extent): Tile =
    CroppedTile(this, RasterExtent(srcExtent, cols, rows).gridBoundsFor(extent))

  def downsample(newCols: Int, newRows: Int)(f: CellSet => Int): Tile = {
    val colsPerBlock = math.ceil(cols / newCols.toDouble).toInt
    val rowsPerBlock = math.ceil(rows / newRows.toDouble).toInt

    val tile = ArrayTile.empty(cellType, newCols, newRows)

    trait DownsampleCellSet extends CellSet { def focusOn(col: Int, row: Int): Unit }

    val cellSet =
      new DownsampleCellSet {
        private var focusCol = 0
        private var focusRow = 0

        def focusOn(col: Int, row: Int) = {
          focusCol = col
          focusRow = row
        }

        def foreach(f: (Int, Int)=>Unit): Unit = {
          var col = 0
          while(col < colsPerBlock) {
            var row = 0
            while(row < rowsPerBlock) {
              f(focusCol * colsPerBlock + col, focusRow * rowsPerBlock + row)
              row += 1
            }
            col += 1
          }
        }
      }

    var col = 0
    while(col < newCols) {
      var row = 0
      while(row < newRows) {
        cellSet.focusOn(col, row)
        tile.set(col, row, f(cellSet))
        row += 1
      }
      col += 1
    }
    tile
  }

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
    val buff = ArrayBuffer[String]()
    var max = 0
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val v = get(col, row)
        val s = if (isNoData(v)) "ND" else s"$v"
        max = math.max(max, s.size)
        buff += s
      }
    }

    createAsciiTileString(buff.toArray, max)
  }

  /**
    * Return ascii art of this raster. The single int parameter indicates the
    * number of significant digits to be printed.
    */
  def asciiDrawDouble(significantDigits: Int = Int.MaxValue): String = {
    val buff = ArrayBuffer[String]()
    val mc = new java.math.MathContext(significantDigits)
    var max = 0
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val v = getDouble(col, row)
        val s = if (isNoData(v)) "ND" else {
          val s = s"$v"
          if (s.size > significantDigits) BigDecimal(s).round(mc).toString
          else s
        }

        max = math.max(s.size, max)
        buff += s
      }
    }

    createAsciiTileString(buff.toArray, max)
  }

  private def createAsciiTileString(buff: Array[String], maxSize: Int) = {
    val sb = new StringBuilder
    val limit = math.max(6, maxSize)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val s = buff(row * cols + col)
        val pad = " " * math.max(limit - s.length, 1)
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
          s += "%02X".formatLocal(Locale.ENGLISH, z)
        }
      }
      s += "\n"
    }
    s
  }
}
