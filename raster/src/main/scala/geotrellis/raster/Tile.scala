/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster

import spire.syntax.cfor._
import geotrellis.util._

import java.util.Locale
import scala.collection.mutable.ArrayBuffer
import scala.math.BigDecimal

/**
  * Base trait for a Tile.
  */
trait Tile extends CellGrid with IterableTile with MappableTile[Tile] with LazyLogging {

  /**
    * Execute a function at each pixel of a [[Tile]].  Two functions
    * are given: an integer version which is used if the tile is an
    * integer-tile, and the other in the case of a floating-tile.
    *
    * @param  f  A function from Int to Unit
    * @param  g  A function from Double to Unit
    */
  def dualForeach(f: Int => Unit)(g: Double => Unit): Unit =
    if (cellType.isFloatingPoint) foreachDouble(g) else foreach(f)

  /**
    * Conditionally execute (or don't) the given function at each
    * pixel of a [[Tile]], depending on whether that pixel is NODATA or
    * not.  The result of the mapping is returned as a tile.
    *
    * @param  f  A function from Int to Int
    */
  def mapIfSet(f: Int => Int): Tile =
    map { i =>
      if(isNoData(i)) i
      else f(i)
    }

  /**
    * Conditionally execute (or don't) the given function at each
    * pixel of a [[Tile]], depending on whether that pixel is NODATA or
    * not.  The result of the mapping is returned as a tile.
    *
    * @param  f  A function from Double to Double
    */
  def mapIfSetDouble(f: Double => Double): Tile =
    mapDouble { d =>
      if(isNoData(d)) d
      else f(d)
    }

  /**
    * Map one of the two given functions across the [[Tile]] to
    * produce a new one.  One of the functions is from Int to Int, and
    * the other from Double to Double.
    *
    * @param  f  A function from Int to Int
    * @param  g  A function from Double to Double
    */
  def dualMap(f: Int => Int)(g: Double => Double): Tile =
    if (cellType.isFloatingPoint) mapDouble(g) else map(f)

  /**
    * Conditionally map across the [[Tile]] with one of two functions,
    * depending on whether the tile is an integer- or a floating-tile.
    * A pixel is mapped only if it is set.
    *
    * @param  f  A function from Int to Int
    * @param  g  A function from Double to Double
    */
  def dualMapIfSet(f: Int => Int)(g: Double => Double): Tile =
    if (cellType.isFloatingPoint) mapIfSetDouble(g) else mapIfSet(f)

  /**
    * Combine two [[Tile]]s together using one of two given functions.
    * If the union of the types of the two cells is floating-point,
    * then the floating function is used, otherwise the integer
    * function is used.
    *
    * @param  r2  The tile to combine with the present one
    * @param  f   The integer function
    * @param  g   The double function
    */
  def dualCombine(r2: Tile)(f: (Int, Int) => Int)(g: (Double, Double) => Double): Tile =
    if (cellType.union(r2.cellType).isFloatingPoint) combineDouble(r2)(g) else combine(r2)(f)

  /**
    * Create a mutable copy of this tile
    */
  def mutable: MutableArrayTile

  /** Converts the cell type of the tile.
    *
    * @note    This will immediately iterate over the tile and allocate a new
    *          copy of data; this should be a performance consideration.
    */
  def convert(cellType: CellType): Tile

  def withNoData(noDataValue: Option[Double]): Tile

  /** Changes the interpretation of the tile cells through changing NoData handling and optionally cell data type.
    * If [[DataType]] portion of the [[CellType]] is unchanged the tile data is not duplicated through conversion.
    * If cell [[DataType]] conversion is required it is done in a naive way, without considering NoData handling.
    *
    * @param newCellType CellType to be used in interpreting existing cells
    */
  def interpretAs(newCellType: CellType): Tile

  /**
    * Get value at given coordinates.
    */
  def get(col: Int, row: Int): Int

  /**
    * Get value at given coordinates.
    */
  def getDouble(col: Int, row: Int): Double

  /**
    * Convert the present [[Tile]] to an [[ArrayTile]].
    */
  def toArrayTile(): ArrayTile

  /**
    * Return the data behind this [[Tile]], or a copy, as an Array of
    * integers.
    */
  def toArray(): Array[Int]

  /**
    * Return the data behind this [[Tile]], or a copy, as an Array of
    * doubles.
    */
  def toArrayDouble(): Array[Double]

  /**
    * Return the data behind this [[Tile]], or a copy, as an Array of
    * bytes.
    */
  def toBytes(): Array[Byte]

  /**
    * Execute the given function at each pixel of the present
    * [[Tile]].
    */
  def foreach(f: Int=>Unit): Unit

  /**
    * Execute the given function at each pixel of the present
    * [[Tile]].
    */
  def foreachDouble(f: Double=>Unit): Unit

  /**
    * Map the given function across the present [[Tile]].  The result
    * is another Tile.
    *
    * Values can also be mapped with "class-break logic":
    * {{{
    * import geotrellis.raster.render.BreakMap
    *
    * // Maps break values to result values
    * val m: Map[Int, Int] = ...
    * val t: Tile = ...
    *
    * // BreakMap extends `Function1`
    * t.map(BreakMap.i2i(m))
    * }}}
    * If `Tile` above had an underlying floating [[CellType]],
    * then the transformation would effectively be from `Double => Int`.
    */
  def map(f: Int => Int): Tile

  /**
    * Combine the given [[Tile]] with the present one using the given
    * function.
    */
  def combine(r2: Tile)(f: (Int, Int) => Int): Tile

  /**
    * Map the given function across the present [[Tile]].  The result
    * is another Tile.
    *
    * Values can also be mapped with "class-break logic":
    * {{{
    * import geotrellis.raster.render.BreakMap
    *
    * // Maps break values to result values
    * val m: Map[Double, Double] = ...
    * val t: Tile = ...
    *
    * // BreakMap extends `Function1`
    * t.mapDouble(BreakMap.i2i(m))
    * }}}
    * If `Tile` above had an underlying integer [[CellType]],
    * then the transformation would effectively be from `Int => Double`.
    */
  def mapDouble(f: Double => Double): Tile

  /**
    * Combine the given [[Tile]] with the present one using the given
    * function.
    */
  def combineDouble(r2: Tile)(f: (Double, Double) => Double): Tile

  def isNoDataTile: Boolean = {
    var (c, r) = (0, 0)
    while (r < rows) {
      while(c < cols) {
        if(cellType.isFloatingPoint) { if (isData(getDouble(c, r))) return false }
        else { if(isData(get(c, r))) return false }
        c += 1
      }
      c = 0; r += 1
    }

    true
  }

  /**
    * Normalizes the values of this raster, given the current min and
    * max, to a new min and max.
    *
    * @param oldMin  Old minimum value
    * @param oldMax  Old maximum value
    * @param newMin  New minimum value
    * @param newMax  New maximum value
    */
  def normalize(oldMin: Int, oldMax: Int, newMin: Int, newMax: Int): Tile = {
    val dnew = newMax - newMin
    val dold = oldMax - oldMin
    if(dold <= 0 || dnew <= 0) { sys.error(s"Invalid parameters: $oldMin, $oldMax, $newMin, $newMax") }
    mapIfSet(z => ( ((z - oldMin) * dnew) / dold ) + newMin)
  }

  /**
    * Normalizes the values of this raster, given the current min and
    * max, to a new min and max.
    *
    * @param oldMin  Old minimum value
    * @param oldMax  Old maximum value
    * @param newMin  New minimum value
    * @param newMax  New maximum value
    */
  def normalize(oldMin: Double, oldMax: Double, newMin: Double, newMax: Double): Tile = {
    val dnew = newMax - newMin
    val dold = oldMax - oldMin
    if(dold <= 0 || dnew <= 0) { sys.error(s"Invalid parameters: $oldMin, $oldMax, $newMin, $newMax") }
    mapIfSetDouble(z => ( ((z - oldMin) * dnew) / dold ) + newMin)
  }

  /**
    * Rescale the values in this [[Tile]] so that they are between the
    * two given values.
    */
  def rescale(newMin: Int, newMax: Int): Tile = {
    val (min, max) = findMinMax
    normalize(min, max, newMin, newMax)
  }

  /**
    * Rescale the values in this [[Tile]] so that they are between the
    * two given values.
    */
  def rescale(newMin: Double, newMax: Double): Tile = {
    val (min, max) = findMinMaxDouble
    normalize(min, max, newMin, newMax)
  }

  /**
    * Reduce the resolution of the present [[Tile]] to the given
    * number of columns and rows.  A new Tile is returned.
    *
    * @param  newCols  The number of columns in the new Tile
    * @param  newRows  The number of rows in the new Tile
    */
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
    * @note Currently does not support double valued raster data types
    *       (FloatConstantNoDataCellType,
    *       DoubleConstantNoDataCellType). Calling findMinMax on
    *       rasters of those types will give the integer min and max
    *       of the rounded values of their cells.
    */
  def findMinMax: (Int, Int) = {
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
  def findMinMaxDouble: (Double, Double) = {
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
}
