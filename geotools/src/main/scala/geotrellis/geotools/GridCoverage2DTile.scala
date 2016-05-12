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

import java.awt.image.DataBuffer
import scala.collection.JavaConverters._


/**
  * The [[GridCoverage2DTile]] object, houses a constructor.
  */
object GridCoverage2DTile {

  /**
    * Takes a [[GridCoverage2D]] and an index, and produces a
    * singleband tile in which only the given band is accessible.
    *
    * @param  gridCoverage2D  The GeoTools GridCoverage2D object
    * @param  bandIndex       The index in gridCoverage2D to expose as the sole band of this tile
    */
  def apply(gridCoverage: GridCoverage2D, bandIndex: Int) =
    new GridCoverage2DTile(gridCoverage, bandIndex)

  /**
    * Given a [[GridCoverage2D]] and an index, this function
    * optionally produces the unique NODATA value of the band (if
    * there is one), otherwise it produces None or raises an
    * exception.
    *
    * @param  gridCoverage2D  The GeoTools GridCoverage2D object
    * @param  bandIndex       The index in gridCoverage2D to expose as the sole band of this tile
    */
  def noData(gridCoverage: GridCoverage2D, bandIndex: Int): Option[Double] = {
    val sd = gridCoverage.getSampleDimension(bandIndex)

    sd.getNoDataValues match {
      case null =>
        if (sd.getCategories.asScala.exists(_.toString.contains("NaN"))) Some(Double.NaN)
        else None
      case Array(nd) => Some(nd)
      case _ => throw new Exception("Only know how to handle one NODATA value")
    }
  }

  /**
    * Given a [[GridCoverage2D]] and an index, this function return
    * the Geotrellis [[CellType]] that best approximates that of the
    * given layer.
    *
    * @param  gridCoverage2D  The GeoTools GridCoverage2D object
    * @param  bandIndex       The index in gridCoverage2D to expose as the sole band of this tile
    */
  def cellType(gridCoverage: GridCoverage2D, bandIndex: Int): CellType = {
    val noDataValue = noData(gridCoverage, bandIndex)
    val renderedImage = gridCoverage.getRenderedImage
    val buffer = renderedImage.getData.getDataBuffer
    val typeEnum = buffer.getDataType

    (noDataValue, typeEnum) match {
      case (None, DataBuffer.TYPE_BYTE) => UByteCellType
      case (None, DataBuffer.TYPE_USHORT) => UShortCellType
      case (None, DataBuffer.TYPE_SHORT) => ShortCellType
      case (None, DataBuffer.TYPE_INT) => IntCellType
      case (None, DataBuffer.TYPE_FLOAT) => FloatCellType
      case (None, DataBuffer.TYPE_DOUBLE) => DoubleCellType
      case (Some(nd), DataBuffer.TYPE_BYTE) =>
        val byte = nd.toByte
        if (byte == ubyteNODATA) UByteConstantNoDataCellType
        else UByteUserDefinedNoDataCellType(byte)
      case (Some(nd), DataBuffer.TYPE_USHORT) =>
        val short = nd.toShort
        if (short == ushortNODATA) UShortConstantNoDataCellType
        else UShortUserDefinedNoDataCellType(short)
      case (Some(nd), DataBuffer.TYPE_SHORT) =>
        val short = nd.toShort
        if (short == shortNODATA) ShortConstantNoDataCellType
        else ShortUserDefinedNoDataCellType(short)
      case (Some(nd), DataBuffer.TYPE_INT) =>
        val int = nd.toInt
        if (int == NODATA) IntConstantNoDataCellType
        else IntUserDefinedNoDataCellType(int)
      case (Some(nd), DataBuffer.TYPE_FLOAT) =>
        val float = nd.toFloat
        if (float == floatNODATA) FloatConstantNoDataCellType
        else FloatUserDefinedNoDataCellType(float)
      case (Some(nd), DataBuffer.TYPE_DOUBLE) =>
        val double = nd.toDouble
        if (double == doubleNODATA) DoubleConstantNoDataCellType
        else DoubleUserDefinedNoDataCellType(double)
      case _ => throw new Exception("Unknown or Undefined Cell Type")
    }
  }
}

/**
  * A Geotrellis [[Tile]]-derived class that wraps a GeoTools
  * GridCoverage2D object.
  *
  * @param  gridCoverage  The GeoTools GridCoverage2D object to wrap
  * @param  bandIndex     The index of gridCoverage2D to expose as the sole band of this tile
  */
class GridCoverage2DTile(gridCoverage: GridCoverage2D, bandIndex: Int) extends Tile {

  val renderedImage = gridCoverage.getRenderedImage
  val buffer = renderedImage.getData.getDataBuffer
  val sampleModel = renderedImage.getSampleModel

  private def bandCount = renderedImage.getSampleModel.getNumBands
  private val _array = Array.ofDim[Int](bandCount)
  private val _arrayDouble = Array.ofDim[Double](bandCount)

  val cellType: CellType = GridCoverage2DTile.cellType(gridCoverage, bandIndex)

  val rows: Int = renderedImage.getHeight

  val cols: Int = renderedImage.getWidth

  /**
    * Execute an [[DoubleTileVisitor]] at each cell of the
    * [[GridCoverage2DTile]].
    *
    * @param  visitor  A DoubleTileVisitor
    */
  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = {
    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        visitor(col, row, getDouble(col, row))
        row += 1
      }
      col += 1
    }
  }

  /**
    * Execute an [[IntTileVisitor]] at each cell of the
    * [[GridCoverage2DTile]].
    *
    * @param  visitor  An IntTileVisitor
    */
  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        visitor(col, row, get(col, row))
        row += 1
      }
      col += 1
    }
  }

  /**
    * Map an [[DoubleTileMapper]] over the present tile.
    *
    * @param   mapper  The mapper
    * @return          The result, a [[Tile]]
    */
  def mapDoubleMapper(mapper: DoubleTileMapper): Tile = {
    val tile = ArrayTile.alloc(cellType.union(DoubleCellType), cols, rows)

    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        tile.setDouble(col, row, mapper(col, row, getDouble(col, row)))
        row += 1
      }
      col += 1
    }

    tile
  }

  /**
    * Map an [[IntTileMapper]] over the present tile.
    *
    * @param   mapper  The mapper
    * @return          The result, a [[Tile]]
    */
  def mapIntMapper(mapper: IntTileMapper): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)

    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        tile.set(col, row, mapper(col, row, get(col, row)))
        row += 1
      }
      col += 1
    }

    tile
  }

  /**
    * Combine the cells of an [[GridCoverage2DTile]] and a [[Tile]]
    * into a new Tile using the given function. For every (x, y) cell
    * coordinate, get each of the Tiles' integer value, map them to a
    * new value, and assign it to the output's (x, y) cell.
    *
    * @param   other  The other Tile
    * @param   f      A function from (Int, Int) to Int
    * @return         The result, an Tile
    */
  def combine(other: Tile)(f: (Int, Int) => Int): Tile = {
    (this, other).assertEqualDimensions

    val tile = ArrayTile.alloc(cellType.union(other.cellType), cols, rows)

    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        tile.set(col, row, f(get(col, row), other.get(col, row)))
        row += 1
      }
      col += 1
    }

    tile
  }

  /**
    * Combine the cells of an [[GridCoverage2DTile]] and a [[Tile]]
    * into a new Tile using the given function. For every (x, y) cell
    * coordinate, get tiles' double values, map them to a new value,
    * and assign it to the output's (x, y) cell.
    *
    * @param   other  The other Tile
    * @param   f      A function from (Double, Double) to Double
    * @return         The result, an Tile
    */
  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = {
    (this, other).assertEqualDimensions

    val tile = ArrayTile.alloc(cellType.union(other.cellType), cols, rows)

    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        tile.setDouble(col, row, f(getDouble(col, row), other.getDouble(col, row)))
        row += 1
      }
      col += 1
    }

    tile
  }

  /**
    * Returns a [[Tile]] equivalent to the present
    * [[GridCoverage2DTile]], except with cells of the given type.
    *
    * @param   cellType  The type of cells that the result should have
    * @return            The new Tile
    */
  def convert(newCellType: CellType): ArrayTile = {
    val tile = ArrayTile.alloc(newCellType, cols, rows)

    if (cellType.isFloatingPoint) {
      var col = 0; while (col < cols) {
        var row = 0; while (row < rows) {
          tile.setDouble(col, row, getDouble(col, row))
          row += 1
        }
        col += 1
      }
    }
    else {
      var col = 0; while (col < cols) {
        var row = 0; while (row < rows) {
          tile.set(col, row, get(col, row))
          row += 1
        }
        col += 1
      }
    }

    tile
  }

  /**
    * Execute a function on each cell of the [[GridCoverage2DTile]].
    *
    * @param  f  A function from Int to Unit.  Presumably, the function is executed for side-effects.
    */
  def foreach(f: Int => Unit): Unit = {
    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        f(get(col, row))
        row += 1
      }
      col += 1
    }
  }

  /**
    * Execute a function on each cell of the [[GridCoverage2DTile]].
    *
    * @param  f  A function from Double to Unit.  Presumably, the function is executed for side-effects.
    */
  def foreachDouble(f: Double => Unit): Unit = {
    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        f(getDouble(col, row))
        row += 1
      }
      col += 1
    }
  }

  /**
    * Fetch the datum at the given column and row of the
    * [[GridCoverage2DTile]].
    *
    * @param   col  The column
    * @param   row  The row
    * @return       The Int datum found at the given location
    */
  def get(col: Int,row: Int): Int =
    sampleModel.getPixel(col, row, _array.clone, buffer)(bandIndex)

  /**
    * Fetch the datum at the given column and row of the
    * [[GridCoverage2DTile]].
    *
    * @param   col  The column
    * @param   row  The row
    * @return       The Double datum found at the given location
    */
  def getDouble(col: Int,row: Int): Double =
    sampleModel.getPixel(col, row, _arrayDouble.clone, buffer)(bandIndex)

  /**
    * Map each cell in the given tile to a new one, using the given
    * function.
    *
    * @param   f  A function from Int to Int, executed at each point of the tile
    * @return     The result, a [[Tile]]
    */
  def map(f: Int => Int): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)

    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        tile.set(col, row, f(get(col, row)))
        row += 1
      }
      col += 1
    }

    tile
  }

  /**
    * Map each cell in the given tile to a new one, using the given
    * function.
    *
    * @param   f  A function from Double to Double, executed at each point of the tile
    * @return     The result, a [[Tile]]
    */
  def mapDouble(f: Double => Double): Tile = {
    val tile = ArrayTile.alloc(cellType.union(DoubleCellType), cols, rows)

    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        tile.setDouble(col, row, f(getDouble(col, row)))
        row += 1
      }
      col += 1
    }

    tile
  }

  /**
    * Return an [[ArrayTile]] whose contents are taken from the
    * present [[GridCoverage2DTile]].  This is fairly expensive.
    */
  def toArrayTile(): ArrayTile = convert(cellType)

  /**
    * Return a [[MutableArrayTile]] whose contents are taken from the
    * present [[GridCoverage2DTile]].  This is fairly expensive.
    */
  def mutable: MutableArrayTile = toArrayTile.mutable

  /**
    * Return the contents of the present [[GridCoverage2DTile]] as an
    * array of integers.  This is fairly expensive.
    */
  def toArray(): Array[Int] = toArrayTile.toArray

  /**
    * Return the contents of the present [[GridCoverage2DTile]] as an
    * array of doubles.  This is fairly expensive.
    */
  def toArrayDouble(): Array[Double] = convert(DoubleCellType).toArrayDouble

  /**
    * Return the contents of the present [[GridCoverage2DTile]] as an
    * array of bytes.  This is fairly expensive.
    */
  def toBytes(): Array[Byte] = toArrayTile.toBytes

}
