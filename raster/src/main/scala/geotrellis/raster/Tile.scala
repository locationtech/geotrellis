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

import spire.syntax.cfor._

import java.util.Locale

/**
 * Base trait for a Tile.
 */
trait Tile {

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

  val cols: Int
  val rows: Int
  lazy val dimensions: (Int, Int) = (cols, rows)
  lazy val size = cols * rows

  val cellType: CellType

  def convert(cellType: CellType): Tile = 
    LazyConvertedTile(this, cellType)

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
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        f(get(col, row))
      }
    }

  def foreachDouble(f: Double=>Unit): Unit =
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
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

  def warp(source: Extent, target: RasterExtent): Tile

  def warp(source: Extent, target: Extent): Tile =
    warp(source, RasterExtent(source, cols, rows).createAligned(target))

  def warp(source: Extent, targetCols: Int, targetRows: Int): Tile =
    warp(source, RasterExtent(source, targetCols, targetRows))

  /** Only changes the resolution */
  def warp(targetCols: Int, targetRows: Int): Tile =
    warp(Extent(0.0, 0.0, 1.0, 1.0), targetCols, targetRows)

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
          s += "%02X".formatLocal(Locale.ENGLISH, z)
        }
      }
      s += "\n"
    }
    s
  }
}
