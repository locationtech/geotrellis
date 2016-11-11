package geotrellis.raster

import geotrellis.raster.op.ConvertOp
import spire.syntax.cfor._

class LazyConvertTile(base: Tile, op: ConvertOp) extends Tile with TileLike {
  def convert(ct: CellType): Tile =
    new LazyConvertTile(base, ConvertOp(op, cellType))

  def withNoData(noDataValue: Option[Double]): Tile = {
    val newCellType = op.cellType.withNoData(noDataValue)
    new LazyConvertTile(base, ConvertOp(op.src, newCellType))
  }

  def interpretAs(newCellType: CellType): Tile =
    new LazyConvertTile(base, ConvertOp(op.src, newCellType))

  def get(col: Int, row: Int): Int = op.get(col, row)

  def getDouble(col: Int, row: Int): Double = op.getDouble(col, row)

  def cellType = op.cellType

  def cols = base.cols

  def rows = base.rows

  // TODO: Should this be lifted to Tile interface ?
  private def prototype(ct: CellType): MutableArrayTile =
    ArrayTile.alloc(ct, cols, rows)

  def map(f: (Int) => Int): Tile = {
    val output = prototype(cellType)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        output.set(col, row, get(col, row))
      }
    }
    output
  }

  def mapDouble(f: Double => Double): Tile = {
    val output = prototype(cellType)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        output.setDouble(col, row, getDouble(col, row))
      }
    }
    output
  }

  def combine(other: Tile)(f: (Int, Int) => Int): Tile = {
    (this, other).assertEqualDimensions
    val output = prototype(cellType union other.cellType)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        output.set(col, row, f(get(col, row), other.get(col, row)))
      }
    }
    output
  }

  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = {
    (this, other).assertEqualDimensions
    val output = prototype(cellType union other.cellType)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        output.setDouble(col, row, f(getDouble(col, row), other.getDouble(col, row)))
      }
    }
    output
  }

  def mapIntMapper(mapper: IntTileMapper): Tile = {
    val output = prototype(cellType)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        output.set(col, row, mapper(col, row, get(col, row)))
      }
    }
    output
  }

  def mapDoubleMapper(mapper: DoubleTileMapper) = {
    val output = prototype(cellType)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        output.setDouble(col, row, mapper(col, row, getDouble(col, row)))
      }
    }
    output
  }

  def mutable: MutableArrayTile = op.toTile(cellType).mutable

  def toArrayTile(): ArrayTile = mutable

  def toBytes(): Array[Byte] = toArrayTile().toBytes()

  def toArrayDouble(): Array[Double] = {
    val output = Array.ofDim[Double](cols * rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        output(row * cols + col) = getDouble(col, row)
      }
    }
    output
  }

  def toArray(): Array[Int] = {
    val output = Array.ofDim[Int](cols * rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        output(row * cols + col) = get(col, row)
      }
    }
    output
  }
}
