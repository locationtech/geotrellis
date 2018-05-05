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

import geotrellis.raster.resample._
import geotrellis.vector.Extent

import spire.syntax.cfor._

/**
  * [DelayedConversionTile]] represents a tile that wraps an inner tile,
  * and for any operation that returns a Tile, returns an ArrayTile with
  * a cell type of the target cell type.
  */
class DelayedConversionMultibandTile(inner: MultibandTile, override val targetCellType: CellType)
  extends MultibandTile with MacroMultibandCombiners {

  private def validateBand(i: Int) = assert(i < bandCount, s"Band index out of bounds. Band Count: $bandCount Requested Band Index: $i")

  val cols = inner.cols
  val rows = inner.rows

  def cellType: CellType =
    inner.cellType

  def convert(cellType: CellType): MultibandTile =
    inner.convert(cellType)

  def withNoData(noDataValue: Option[Double]): MultibandTile =
    inner.withNoData(noDataValue)

  def interpretAs(newCellType: CellType): MultibandTile =
    withNoData(None).convert(newCellType)

  def bandCount: Int = inner.bandCount
  def band(bandIndex: Int): Tile = inner.band(bandIndex)
  def bands: Vector[Tile] = inner.bands

  def subsetBands(bandSequence: Seq[Int]): MultibandTile =
    inner.subsetBands(bandSequence)

  def foreach(f: (Int, Int) => Unit): Unit = inner.foreach(f)
  def foreachDouble(f: (Int, Double) => Unit): Unit = inner.foreachDouble(f)
  def foreach(b0: Int)(f: Int => Unit): Unit = inner.foreach(b0)(f)
  def foreachDouble(b0: Int)(f: Double => Unit): Unit = inner.foreachDouble(b0)(f)
  def foreach(f: Array[Int] => Unit): Unit = inner.foreach(f)
  def foreachDouble(f: Array[Double] => Unit): Unit = inner.foreachDouble(f)

  /**
    * Map over a subset of the bands of an [[ArrayMultibandTile]] to
    * create a new integer-valued [[MultibandTile]] that have the target [[CellType]].
    *
    * @param   subset  A sequence containing the subset of bands that are of interest.
    * @param   f       A function to map over the bands.
    * @return          A MultibandTile containing the result
    */
  def map(subset: Seq[Int])(f: (Int, Int) => Int): MultibandTile = {
    val newBands = Array.ofDim[Tile](bandCount)
    val set = subset.toSet

    subset.foreach({ b =>
      require(0 <= b && b < bandCount, "All elements of subset must be present")
    })

    (0 until bandCount).foreach({ b =>
      val bandTile = band(b)

      if (set.contains(b))
        newBands(b) = bandTile.delayedConversion(targetCellType).map({ z => f(b, z) })
      else if (targetCellType.isFloatingPoint)
        newBands(b) = bandTile.convert(targetCellType)
      else
        newBands(b) = bandTile.convert(targetCellType)
    })

    ArrayMultibandTile(newBands)
  }

  /**
    * Map over a subset of the bands of an [[ArrayMultibandTile]] to
    * create a new double-valued [[MultibandTile]] that has the target [[CellType]].
    *
    * @param   subset  A sequence containing the subset of bands that are of interest.
    * @param   f       A function to map over the bands.
    * @return          A MultibandTile containing the result
    */
  def mapDouble(subset: Seq[Int])(f: (Int, Double) => Double): MultibandTile = {
    val newBands = Array.ofDim[Tile](bandCount)
    val set = subset.toSet

    subset.foreach({ b =>
      require(0 <= b && b < bandCount, "All elements of subset must be present")
    })

    (0 until bandCount).foreach({ b =>
      val bandTile = band(b)

      if (set.contains(b))
        newBands(b) = bandTile.delayedConversion(targetCellType).mapDouble({ z => f(b, z) })
      else if (targetCellType.isFloatingPoint)
        newBands(b) = bandTile.convert(targetCellType)
      else
        newBands(b) = bandTile.convert(targetCellType)
    })

    ArrayMultibandTile(newBands)
  }

  /**
    * Map each band's int value.
    *
    * @param   f  Function that takes in a band number and a value, and returns the mapped value for that cell value.
    * @return     A [[MultibandTile]] containing the result
    */
  def map(f: (Int, Int) => Int): MultibandTile = {
    val newBands = Array.ofDim[Tile](bandCount)
    cfor(0)(_ < bandCount, _ + 1) { i =>
      newBands(i) = band(i).delayedConversion(targetCellType).map { z => f(i, z) }
    }

    ArrayMultibandTile(newBands)
  }

  /**
    * Map each band's double value.
    *
    * @param   f  Function that takes in a band number and a value, and returns the mapped value for that cell value.
    * @return     A [[MultibandTile]] containing the result
    */
  def mapDouble(f: (Int, Double) => Double): MultibandTile = {
    val newBands = Array.ofDim[Tile](bandCount)
    cfor(0)(_ < bandCount, _ + 1) { i =>
      newBands(i) = band(i).delayedConversion(targetCellType).mapDouble { z => f(i, z) }
    }

    ArrayMultibandTile(newBands)
  }

  /**
    * Map a single band's int value.
    *
    * @param   bandIndex  Band index to map over.
    * @param   f          Function that takes in a band number and a value, and returns the mapped value for that cell value.
    * @return             A [[MultibandTile]] containing the result
    */
  def map(b0: Int)(f: Int => Int): MultibandTile = {
    validateBand(b0)
    val newBands = Array.ofDim[Tile](bandCount)
    cfor(0)(_ < bandCount, _ + 1) { i =>
      if(i == b0) { newBands(i) = band(i).delayedConversion(targetCellType).map(f) }
      else { newBands(i) = band(i).convert(targetCellType) }
    }

    ArrayMultibandTile(newBands)
  }

  /**
    * Map each band's double value.
    *
    * @param   f  Function that takes in a band number and a value, and returns the mapped value for that cell value.
    * @return     A [[MultibandTile]] containing the result
    */
  def mapDouble(b0: Int)(f: Double => Double): MultibandTile = {
    validateBand(b0)
    val newBands = Array.ofDim[Tile](bandCount)
    cfor(0)(_ < bandCount, _ + 1) { i =>
      if(i == b0) { newBands(i) = band(i).delayedConversion(targetCellType).mapDouble(f) }
      else { newBands(i) = band(i).convert(targetCellType) }
    }

    ArrayMultibandTile(newBands)
  }

  /**
    * Combine a subset of the bands of a [[ArrayMultibandTile]] into a
    * new integer-valued [[MultibandTile]] using the function f.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest.
    * @param    f        A function to combine the bands.
    * @return            The [[Tile]] that results from combining the bands. This will be an [[ArrayTile]] that has the target [[CellType]].
    */
  def combine(subset: Seq[Int])(f: Seq[Int] => Int): Tile = {
    subset.foreach({ b => require(0 <= b && b < bandCount, "All elements of subset must be present") })
    val subsetSize = subset.size
    val subsetArray = subset.toArray

    val result = ArrayTile.empty(targetCellType, cols, rows)
    val values: Array[Int] = Array.ofDim(subsetSize)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < subsetSize, _ + 1) { b =>
          values(b) = inner.bands(subsetArray(b)).get(col, row)
        }
        result.set(col, row, f(values))
      }
    }

    result
  }

  /**
    * Combine a subset of the bands of a [[ArrayMultibandTile]] into a
    * new double-valued [[MultibandTile]] using the function f.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest.
    * @param    f        A function to combine the bands.
    * @return            The [[Tile]] that results from combining the bands. This will be an [[ArrayTile]] that has the target [[CellType]].
    */
  def combineDouble(subset: Seq[Int])(f: Seq[Double] => Double): Tile = {
    subset.foreach({ b => require(0 <= b && b < bandCount, "All elements of subset must be present") })
    val subsetSize = subset.size
    val subsetArray = subset.toArray

    val result = ArrayTile.empty(targetCellType, cols, rows)
    val values: Array[Double] = Array.ofDim(subsetSize)

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < subsetSize, _ + 1) { b =>
          values(b) = inner.bands(subsetArray(b)).getDouble(col, row)
        }
        result.setDouble(col, row, f(values))
      }
    }
    result
  }

  /**
    * Combine each int band value for each cell.  This method will be
    * inherently slower than calling a method with explicitly stated
    * bands, so if you have as many or fewer bands to combine than an
    * explicit method call, use that.
    *
    * @param   f  A function from Array[Int] to Int.  The array contains the values of each band at a particular point.
    * @return     The [[Tile]] that results from combining the bands. This will be an [[ArrayTile]] that has the target [[CellType]].
    */
  def combine(f: Array[Int] => Int): Tile = {
    val result = ArrayTile.empty(targetCellType, cols, rows)
    val arr = Array.ofDim[Int](bandCount)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < bandCount, _ + 1) { b =>
          arr(b) = band(b).get(col, row)
        }
        result.set(col, row, f(arr))
      }
    }
    result
  }

  /**
    * Combine two int band value for each cell.
    *
    * @param   b0  The index of the first band to combine.
    * @param   b1  The index of the second band to combine.
    * @param   f   A function from (Int, Int) to Int.  The tuple contains the respective values of the two bands at a particular point.
    * @return      The [[Tile]] that results from combining the bands. This will be an [[ArrayTile]] that has the target [[CellType]].
    */
  def combine(b0: Int, b1: Int)(f: (Int, Int) => Int): Tile = {
    val band1 = band(b0)
    val band2 = band(b1)
    val result = ArrayTile.empty(targetCellType, cols, rows)

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        result.set(col, row, f(band1.get(col, row), band2.get(col, row)))
      }
    }
    result
  }

  /**
    * Combine each double band value for each cell.  This method will
    * be inherently slower than calling a method with explicitly
    * stated bands, so if you have as many or fewer bands to combine
    * than an explicit method call, use that.
    *
    * @param   f  A function from Array[Double] to Double.  The array contains the values of each band at a particular point.
    * @return     The [[Tile]] that results from combining the bands. This will be an [[ArrayTile]] that has the target [[CellType]].
    */
  def combineDouble(f: Array[Double] => Double) = {
    val result = ArrayTile.empty(targetCellType, cols, rows)
    val arr = Array.ofDim[Double](bandCount)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < bandCount, _ + 1) { b =>
          arr(b) = band(b).getDouble(col, row)
        }
        result.setDouble(col, row, f(arr))
      }
    }
    result
  }

  /**
    * Combine two double band value for each cell.
    *
    * @param   b0  The index of the first band to combine.
    * @param   b1  The index of the second band to combine.
    * @param   f   A function from (Double, Double) to Double.  The tuple contains the respective values of the two bands at a particular point.
    * @return      The [[Tile]] that results from combining the bands. This will be an [[ArrayTile]] that has the target [[CellType]].
    */
  def combineDouble(b0: Int, b1: Int)(f: (Double, Double) => Double): Tile = {
    val band1 = band(b0)
    val band2 = band(b1)
    val result = ArrayTile.empty(targetCellType, cols, rows)
    val arr = Array.ofDim[Int](bandCount)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        result.setDouble(col, row, f(band1.getDouble(col, row), band2.getDouble(col, row)))
      }
    }
    result
  }

  def toArrayTile: ArrayMultibandTile = inner.toArrayTile
}
