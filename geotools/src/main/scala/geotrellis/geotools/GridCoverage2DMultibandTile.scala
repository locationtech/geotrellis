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

import geotrellis.raster._

import org.geotools.coverage.grid._
import org.geotools.gce.geotiff._
import org.geotools.coverage.grid.io._


/**
  * The GridCoverage2DMultibandTile Object, houses a constructor.
  */
object GridCoverage2DMultibandTile {

  /**
    * Takes a GridCoverage2D and wraps it in a Geotrellis
    * [[MultibandTile]]-derived object.
    *
    * @param  gridCoverage2D  The GeoTools GridCoverage2D object
    */
  def apply(gridCoverage: GridCoverage2D) =
    new GridCoverage2DMultibandTile(gridCoverage)
}

/**
  * Takes a GridCoverage2D and wraps it in a Geotrellis
  * [[MultibandTile]]-derived object.
  *
  * @param  gridCoverage2D  The GeoTools GridCoverage2D object
  */
class GridCoverage2DMultibandTile(gridCoverage: GridCoverage2D)
    extends MultibandTile with MacroMultibandCombiners {

  val renderedImage = gridCoverage.getRenderedImage
  val buffer = renderedImage.getData.getDataBuffer
  val sampleModel = renderedImage.getSampleModel

  private val _array = Array.ofDim[Int](sampleModel.getNumBands)
  private val _arrayDouble = Array.ofDim[Double](sampleModel.getNumBands)
  private def getFiber(col: Int, row: Int) = sampleModel.getPixel(col, row, _array, buffer)
  private def getFiberDouble(col: Int, row: Int) = sampleModel.getPixel(col, row, _arrayDouble, buffer)

  val bandCount: Int = sampleModel.getNumBands

  val cellType: CellType = GridCoverage2DToRaster.cellType(gridCoverage)

  val rows: Int = renderedImage.getHeight

  val cols: Int = renderedImage.getWidth

  /**
    * Returns a band of this [[GridCoverage2DMultibandTile]] as a
    * [[GridCoverage2DTile]].
    *
    * @params  bandIndex  The index of the band to return
    */
  def band(bandIndex: Int) =
    GridCoverage2DTile(gridCoverage, bandIndex)

  /**
    * Returns the bands of this [[GridCoverage2DMultibandTile]] as a
    * Vector of [[GridCoverage2DTile]] objects.
    */
  def bands =
    (0 until bandCount).map({ b => band(b) }).toVector

  /**
    * Given two band indices and a combining function, this method
    * produces a new [[Tile]].  The combining function takes
    * corresponding (integer) values from the two bands produces an
    * integer which is put into the result Tile.
    *
    * @param  b0  The index of the first band
    * @param  b1  The index of the second band
    * @param  f   A function from (Int, Int) to Int
    */
  def combine(b0: Int, b1: Int)(f: (Int, Int) => Int): Tile =
    combine(List(b0, b1))({ xs: Seq[Int] => f(xs.head, xs.tail.head) })

  /**
    * Given a combining function, this method produces a new [[Tile]].
    * The combining function takes corresponding (integer) values from
    * all of the bands of the present [[GridCoverage2DMultibandTile]]
    * and produces an integer which is put into the result Tile.
    *
    * @param  f   A function from Array[Int] to Int
    */
  def combine(f: Array[Int] => Int): Tile =
    combine(0 until bandCount)({ xs: Seq[Int] => f(xs.toArray) })

  /**
    * Given a subset of indices and a combining function, this method
    * produces a new [[Tile]].  The combining function takes
    * corresponding (integer) values from all of the specified bands
    * and produces an integer which is put into the result Tile.
    *
    * @param  subset  The set of indices (given as a sequence)
    * @param  f       A function from (Int, Int) to Int
    */
  def combine(subset: Seq[Int])(f: Seq[Int] => Int): Tile = {
    val result = ArrayTile.alloc(cellType, cols, rows)

    var row = 0; while (row < rows) {
      var col = 0; while (col < cols) {
        val array = getFiber(col, row)
        val data = subset.map({ b => array(b) })
        result.set(col, row, f(data))
        col += 1
      }
      row += 1
    }

    result
  }

  /**
    * Given two band indices and a combining function, this method
    * produces a new [[Tile]].  The combining function takes
    * corresponding (Double) values from the two bands produces a
    * Double which is put into the result Tile.
    *
    * @param  b0  The index of the first band
    * @param  b1  The index of the second band
    * @param  f   A function from (Double, Double) to Double
    */
  def combineDouble(b0: Int, b1: Int)(f: (Double, Double) => Double): Tile =
    combineDouble(List(b0, b1))({ xs: Seq[Double] => f(xs.head, xs.tail.head) })

  /**
    * Given a combining function, this method produces a new [[Tile]].
    * The combining function takes corresponding (Double) values from
    * all of the bands of the present [[GridCoverage2DMultibandTile]]
    * and produces a Double which is put into the result Tile.
    *
    * @param  f   A function from Array[Double] to Double
    */
  def combineDouble(f: Array[Double] => Double): Tile =
    combineDouble(0 until bandCount)({ xs: Seq[Double] => f(xs.toArray) })

  /**
    * Given a subset of indices and a combining function, this method
    * produces a new [[Tile]].  The combining function takes
    * corresponding (Double) values from all of the specified bands
    * and produces a Double which is put into the result Tile.
    *
    * @param  subset  The set of indices (given as a sequence)
    * @param  f       A function from (Double, Double) to Double
    */
  def combineDouble(subset: Seq[Int])(f: Seq[Double] => Double): Tile = {
    val result = ArrayTile.alloc(cellType.union(DoubleCellType), cols, rows)

    var row = 0; while (row < rows) {
      var col = 0; while (col < cols) {
        val array = getFiberDouble(col, row)
        val data = subset.map({ b => array(b) })
        result.setDouble(col, row, f(data))
        col += 1
      }
      row += 1
    }

    result
  }

  /**
    * Execute the given function on each pixel of the given band.
    *
    * @param  b0  The band index
    * @param  f   A function from Int to Unit
    */
  def foreach(b0: Int)(f: Int => Unit): Unit =
    band(b0).foreach(f)

  /**
    * Execute the given function on each pixel of the present
    * [[GridCoverage2DMultibandTile]].  The function takes a band
    * index and a value from the band as input.
    *
    * @param  f  A function from (Int, Int) to Unit
    */
  def foreach(f: (Int, Int) => Unit): Unit = {
    bands.zipWithIndex
      .foreach({ case (tile, i) =>
        tile.foreach(f(i,_))
      })
  }

  /**
    * Execute the given function on each pixel of the given band.
    *
    * @param  b0  The band index
    * @param  f   A function from Double to Unit
    */
  def foreachDouble(b0: Int)(f: Double => Unit): Unit =
    band(b0).foreachDouble(f)

  /**
    * Execute the given function on each pixel of the present
    * [[GridCoverage2DMultibandTile]].  The function takes a band
    * index and a value from the band as input.
    *
    * @param  f  A function from (Int, Double) to Unit
    */
  def foreachDouble(f: (Int, Double) => Unit): Unit = {
    bands.zipWithIndex
      .foreach({ case (tile, i) =>
        tile.foreachDouble(f(i,_))
      })
  }

  /**
    * Map the given function over the given band of the present
    * [[GridCoverage2DMultibandTile]].
    *
    * @param  b0  The index of the band
    * @param  f   A function from (Int, Int) to Int
    */
  def map(b0: Int)(f: Int => Int): MultibandTile =
    map(List(b0))({ (_,z) => f(z) })

  /**
    * Map the given function over all of the bands of the present
    * [[GridCoverage2DMultibandTile]].  The function takes an Int
    * (band number) and Int (value) as inputs, and produces an Int as
    * output.
    *
    * @param  f   A function from Int to Int
    */
  def map(f: (Int, Int) => Int): MultibandTile =
    map(0 until bandCount)(f)

  /**
    * Map the given function a given subset of the bands of the
    * present [[GridCoverage2DMultibandTile]].  The function takes an
    * Int (band number) and Int (value) as inputs, and produces an Int
    * as output.
    *
    * @param  subset  A set of band indices, given as a sequence
    * @param  f       A function from (Int, Int) to Int
    */
  def map(subset: Seq[Int])(f: (Int, Int) => Int): MultibandTile = {
    val set = subset.toSet
    val newBands = bands
      .zipWithIndex
      .map({ case (tile, i) =>
        if (set.contains(i))
          tile.map(f(i,_))
        else if (cellType.isFloatingPoint)
          tile.map({ z => z })
        else
          tile
      })
      .toArray

    ArrayMultibandTile(newBands)
  }

  /**
    * Map the given function over the given band of the present
    * [[GridCoverage2DMultibandTile]].
    *
    * @param  b0  The index of the band
    * @param  f   A function from Double to Double
    */
  def mapDouble(b0: Int)(f: Double => Double): MultibandTile =
    mapDouble(List(b0))({ (_,z) => f(z) })

  /**
    * Map the given function over all of the bands of the present
    * [[GridCoverage2DMultibandTile]].  The function takes an Int
    * (band number) and Double (value) as inputs, and produces a
    * Double as output.
    *
    * @param  f   A function from (Int, Double) to Int
    */
  def mapDouble(f: (Int, Double) => Double): MultibandTile =
    mapDouble(0 until bandCount)(f)

  /**
    * Map the given function a given subset of the bands of the
    * present [[GridCoverage2DMultibandTile]].  The function takes an
    * Int (band number) and Double (value) as inputs, and produces a
    * Double as output.
    *
    * @param  subset  A set of band indices, given as a sequence
    * @param  f       A function from (Int, Double) to Double
    */
  def mapDouble(subset: Seq[Int])(f: (Int, Double) => Double): MultibandTile = {
    val set = subset.toSet
    val newBands = bands
      .zipWithIndex
      .map({ case (tile, i) =>
        if (set.contains(i))
          tile.mapDouble(f(i,_))
        else if (cellType.isFloatingPoint)
          tile
        else
          tile.mapDouble({ z => z })
      })
      .toArray

    ArrayMultibandTile(newBands)
  }

  /**
    * Produce a new [[MultibandTile]], based on the present
    * [[GridCoverage2DMultibandTile]], with the given [[CellType]].
    * This is a fairly expensive operation.
    */
  def convert(newCellType: geotrellis.raster.CellType): MultibandTile =
    ArrayMultibandTile(bands.map(_.convert(newCellType)).toArray)

  /**
    * Produce a new [[ArrayMultibandTile]] with only those bands specified.q
    *
    * @param  bandSequence  A set of band indices, given as a sequence
    */
  def subsetBands(bandSequence: Seq[Int]): MultibandTile =
    ArrayMultibandTile(bandSequence.map({ b => GridCoverage2DTile(gridCoverage, b) }).toArray)
}
