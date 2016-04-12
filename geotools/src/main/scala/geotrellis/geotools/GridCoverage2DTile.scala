package geotrellis.geotools

import geotrellis.raster._

import org.geotools.coverage.grid._
import org.geotools.gce.geotiff._
import org.geotools.coverage.grid.io._


// XXX ScalaDocs
object GridCoverage2DTile {
  def apply(gridCoverage: GridCoverage2D, bandIndex: Int) =
    new GridCoverage2DTile(gridCoverage, bandIndex)
}

class GridCoverage2DTile(gridCoverage: GridCoverage2D, bandIndex: Int) extends Tile {

  val renderedImage = gridCoverage.getRenderedImage
  val buffer = renderedImage.getData.getDataBuffer
  val sampleModel = renderedImage.getSampleModel

  private def bandCount = renderedImage.getSampleModel.getNumBands
  private val _array = Array.ofDim[Int](bandCount)
  private val _arrayDouble = Array.ofDim[Double](bandCount)

  val cellType: CellType = GridCoverage2DToRaster.cellType(gridCoverage)

  val rows: Int = renderedImage.getHeight

  val cols: Int = renderedImage.getWidth

  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = {
    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        visitor(col, row, getDouble(col, row))
        row += 1
      }
      col += 1
    }
  }

  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        visitor(col, row, get(col, row))
        row += 1
      }
      col += 1
    }
  }

  def mapDoubleMapper(mapper: DoubleTileMapper): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)

    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        tile.setDouble(col, row, mapper(col, row, getDouble(col, row)))
        row += 1
      }
      col += 1
    }

    tile
  }

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

  def foreach(f: Int => Unit): Unit = {
    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        f(get(col, row))
        row += 1
      }
      col += 1
    }
  }

  def foreachDouble(f: Double => Unit): Unit = {
    var col = 0; while (col < cols) {
      var row = 0; while (row < rows) {
        f(getDouble(col, row))
        row += 1
      }
      col += 1
    }
  }

  def get(col: Int,row: Int): Int =
    sampleModel.getPixel(col, row, _array, buffer)(bandIndex)

  def getDouble(col: Int,row: Int): Double =
    sampleModel.getPixel(col, row, _arrayDouble, buffer)(bandIndex)

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

  def toArrayTile(): ArrayTile = convert(cellType)

  def mutable: MutableArrayTile = toArrayTile.mutable

  def toArray(): Array[Int] = toArrayTile.toArray

  def toArrayDouble(): Array[Double] = convert(DoubleCellType).toArrayDouble

  def toBytes(): Array[Byte] = toArrayTile.toBytes

}
