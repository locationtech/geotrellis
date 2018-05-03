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

import geotrellis.macros._
import geotrellis.raster.summary._
import geotrellis.vector.Extent

import spire.syntax.cfor._

import java.util.Locale

import math.BigDecimal


object MultibandTile {
  /**
    * Creates a multiband tile from a sequence of tiles which
    * represent the bands.  This creates an [[ArrayMultibandTile]],
    * the default implementation of [[MultibandTile]].
    */
  def apply(bands: Tile*): MultibandTile =
    ArrayMultibandTile(bands)

  /**
    * Creates a multiband tile from a sequence of tiles which
    * represent the bands.  This creates an [[ArrayMultibandTile]],
    * the default implementation of [[MultibandTile]].
    */
  def apply(bands: Traversable[Tile]): MultibandTile =
    ArrayMultibandTile(bands)

  /** Creates a multiband tile from a sequence of tiles which represent the bands.
    * This creates an ArrayMultibandTile, the default implementation of MultibandTile.
    */
  def apply(bands: Array[Tile]): MultibandTile =
    ArrayMultibandTile(bands)
}

trait MultibandTile extends CellGrid with MacroCombinableMultibandTile[Tile] with MacroCombineFunctions[Tile, MultibandTile] {
  def bandCount: Int

  /**
    * Retrieve a particular band from the [[MultibandTile]] and return
    * it as a [[Tile]].
    */
  def band(bandIndex: Int): Tile

  /** Safely fetch a band from a [[MultibandTile]], given some band index
    * which may or may not be valid.
    */
  def bandSafe(bandIndex: Int): Option[Tile] =
    if(bandIndex < bandCount && bandIndex >= 0) Some(band(bandIndex)) else None

  /**
    * A vector of all bands in this MultibandTile.
    */
  def bands: Vector[Tile]

  /** Target cell type for generation of new Tile and MultibandTiles.
    */
  protected def targetCellType: CellType = cellType

  /**
    * Retrieve a subset of the bands of the present [[MultibandTile]]
    * as a new [[MultibandTile]].
    */
  def subsetBands(bandSequence: Seq[Int]): MultibandTile

  /**
    * Retrieve a subset of the bands of the present [[MultibandTile]]
    * as a new [[MultibandTile]].
    */
  def subsetBands(bandSequence: Int*)(implicit d: DummyImplicit): MultibandTile =
    subsetBands(bandSequence)

  /**
    * Returns a [[MultibandTile]] equivalent to this one, except with
    * cells of the given type.
    *
    * @param   newCellType  The type of cells that the result should have
    * @return               The new MultibandTile
    */
  def convert(newCellType: CellType): MultibandTile


  /** Return tile tile as raw cell [[MultibandTile]] with No NoData handling */
  def withNoData(noDataValue: Option[Double]): MultibandTile

  /** Changes the interpretation of the tile cells through changing NoData handling and optionally cell data type.
    * If [[DataType]] portion of the [[CellType]] is unchanged the tile data is not duplicated through conversion.
    * If cell [[DataType]] conversion is required it is done in a naive way, without considering NoData handling.
    *
    * @param newCellType CellType to be used in interpreting existing cells
    */
  def interpretAs(newCellType: CellType): MultibandTile

  /**
    * Map over each band, and return a new MultibandTile.
    *
    * @param f      A function to apply to each band, given it's band index.
    *
    * @return       An ArrayMultibandTile with the resulting tiles.
    */
  def mapBands(f: (Int, Tile) => Tile): MultibandTile = {
    val resultBands = Array.ofDim[Tile](bandCount)
    cfor(0)(_ < bandCount, _ + 1) { b =>
      resultBands(b) = f(b, band(b))
    }
    ArrayMultibandTile(resultBands)
  }

  /**
    * Map over a subset of the bands of a multiband tile to create a
    * new integer-valued multiband tile.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest
    * @param    f        A function to map over the bands
    */
  def map(subset: Seq[Int])(f: (Int, Int) => Int): MultibandTile

  /**
    * Map over a subset of the bands of a multiband tile to create a
    * new double-valued multiband tile.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest
    * @param    f        A function to map over the bands
    */
  def mapDouble(subset: Seq[Int])(f: (Int, Double) => Double): MultibandTile

  /**
    * Map each band's int value using a function that takes in a band
    * number and a value, and returns the mapped value for that cell
    * value.
    *
    * @param  f  The function
    */
  def map(f: (Int, Int) => Int): MultibandTile

  /**
    * Map each band's double value using a function that takes in a
    * band number and a value, and returns the mapped value for that
    * cell value.
    *
    * @param  f  The function
    */
  def mapDouble(f: (Int, Double) => Double): MultibandTile

  /**
    * Map a single band's int value using a function that takes in a
    * band number and a value, and returns the mapped value for that
    * cell value.
    *
    * @param  bandIndex  Band index to map over
    * @param  f          The function
    */
  def map(b0: Int)(f: Int => Int): MultibandTile

  /**
    * Map each band's double value using a function that takes in a
    * band number and a value, and returns the mapped value for that
    * cell value.
    *
    * @param  f  The function
    */
  def mapDouble(b0: Int)(f: Double => Double): MultibandTile

  /**
    * Iterate over each band's int value using a function that takes
    * in a band number and a value, and returns the foreached value
    * for that cell value.
    *
    * @param  f  The function
    */
  def foreach(f: (Int, Int) => Unit): Unit

  /**
    * Iterate over each band's double value using a function that
    * takes in a band number and a value, and returns the foreached
    * value for that cell value.
    *
    * @param  f  The function
    */
  def foreachDouble(f: (Int, Double) => Unit): Unit

  /**
    * Iterate over a single band's int value using a function that
    * takes in a band number and a value, and returns the foreached
    * value for that cell value.
    *
    * @param  bandIndex  Band index to foreach over
    * @param  f          The function
    */
  def foreach(b0: Int)(f: Int => Unit): Unit

  /**
    * Iterate over a single band's double value using a function that
    * takes in a band number and a value, and returns the foreached
    * value for that cell value.
    *
    * @param  bandIndex  Band index to foreach over
    * @param  f          The function
    */
  def foreachDouble(b0: Int)(f: Double => Unit): Unit

  /**
    * Multiband iterate over tile's int value using a function that
    * takes in an array of values, and returns the foreached
    * value for that cell value.
    *
    * @param  f          The function
    */
  def foreach(f: Array[Int] => Unit): Unit

  /**
    * Multiband iterate over tile's double value using a function that
    * takes in an array of values, and returns the foreached
    * value for that cell value.
    *
    * @param  f          The function
    */
  def foreachDouble(f: Array[Double] => Unit): Unit

  /**
    * Combine a subset of the bands of a tile into a new
    * integer-valued multiband tile using the function f.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest
    * @param    f        A function to combine the bands
    */
  def combine(subset: Seq[Int])(f: Seq[Int] => Int): Tile

  /**
    * Combine a subset of the bands of a tile into a new double-valued
    * multiband tile using the function f.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest
    * @param    f        A function to combine the bands
    */
  def combineDouble(subset: Seq[Int])(f: Seq[Double] => Double): Tile

  /**
    * Combine each int band value for each cell.  This method will be
    * inherently slower than calling a method with explicitly stated
    * bands, so if you have as many or fewer bands to combine than an
    * explicit method call, use that.
    */
  def combine(f: Array[Int] => Int): Tile

  /**
    * Combine two integer bands value-by-value at each cell.
    */
  def combine(b0: Int, b1: Int)(f: (Int, Int) => Int): Tile

  /**
    * Combine each double band value for each cell.  This method will
    * be inherently slower than calling a method with explicitly
    * stated bands, so if you have as many or fewer bands to combine
    * than an explicit method call, use that.
    */
  def combineDouble(f: Array[Double] => Double): Tile

  /**
    * Combine two double band value for each cell.
    */
  def combineDouble(b0: Int, b1: Int)(f: (Double, Double) => Double): Tile

  /**
    * Convert the present [[MultibandTile]] to an [[MultibandTile]] of [[ArrayTile]]s.
    */
  def toArrayTile(): ArrayMultibandTile
}
