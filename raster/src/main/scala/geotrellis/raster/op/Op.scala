package geotrellis.raster.op

import geotrellis.raster._
import spire.syntax.cfor._

/**
  * Represents a chain of transformation on a Tile.
  * Cells as viewed through this transformations to not have a CellType because they are only be viewed through Int/Double interface.
  * However they may be chained and finally sunk into the result type.
  */
trait Op extends TileLike with Grid {
  def cols: Int

  def rows: Int

  def get(col: Int, row: Int): Int

  def getDouble(col: Int, row: Int): Double

  def map(f: Int => Int): Op =
    new IntMapOp(this, f)

  def mapDouble(f: Double => Double): Op =
    new DoubleMapOp(this, f)

  def combine(other: Op)(f: (Int, Int) => Int): Op =
    new IntCombineOp(this, other, f)

  def combineDouble(other: Op)(f: (Double, Double) => Double): Op =
    new DoubleCombineOp(this, other, f)

  def mapIntMapper(mapper: IntTileMapper) =
    new IntMapperOp(this, mapper)

  def mapDoubleMapper(mapper: DoubleTileMapper) =
    new DoubleMapperOp(this, mapper)

  def toTile(ct: CellType): Tile = {
    val output = ArrayTile.empty(ct, cols, rows)
    if (ct.isFloatingPoint) {
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          output.setDouble(col, row, getDouble(col, row))
        }
      }
    } else {
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          output.set(col, row, get(col, row))
        }
      }
    }
    output
  }
}

object Op {
  implicit def tileToOp(tile: Tile): Op = BoundOp(tile)

  trait Unary { self: Op =>
    def src: Op
    def cols: Int = src.cols
    def rows: Int = src.rows
  }

  trait Binary { self: Op =>
    def left: Op
    def right: Op
    def cols: Int = left.cols
    def rows: Int = left.rows

    // TODO: This makes it hard to have unbound Ops
    require(left.dimensions == right.dimensions, "Cannot combine ops with different dimensions: " +
      s"${left.dimensions} does not match ${right.dimensions}")
  }
}

// Need something to bootstrap the chain
case class BoundOp(tile: Tile) extends Op {
  def cols: Int = tile.cols
  def rows: Int = tile.rows
  def get(col: Int, row: Int): Int = tile.get(col, row)
  def getDouble(col: Int, row: Int): Double = tile.getDouble(col, row)
}

case class IntMapOp(src: Op, f: Int => Int) extends Op with Op.Unary {
  def get(col: Int, row: Int) = f(src.get(col, row))
  def getDouble(col: Int, row: Int) = i2d(f(src.get(col, row)))
}

case class IntMapperOp(src: Op, mapper: IntTileMapper) extends Op with Op.Unary{
  def get(col: Int, row: Int) = mapper(col, row, src.get(col, row))
  def getDouble(col: Int, row: Int) = i2d(mapper(col, row, src.get(col, row)))
}

case class IntCombineOp(left: Op, right: Op, f: (Int, Int) => Int) extends Op with Op.Binary {
  def get(col: Int, row: Int) = f(left.get(col, row), right.get(col, row))
  def getDouble(col: Int, row: Int) = i2d(f(left.get(col, row), right.get(col, row)))
}

case class DoubleMapOp(src: Op, f: Double => Double) extends Op with Op.Unary {
  def get(col: Int, row: Int) = d2i(f(src.getDouble(col, row)))
  def getDouble(col: Int, row: Int) = f(src.getDouble(col, row))
}

case class DoubleMapperOp(src: Op, mapper: DoubleTileMapper) extends Op with Op.Unary {
  def get(col: Int, row: Int) = d2i(mapper(col, row, src.getDouble(col, row)))
  def getDouble(col: Int, row: Int) = mapper(col, row, src.getDouble(col, row))
}

case class DoubleCombineOp(left: Op, right: Op, f: (Double, Double) => Double) extends Op with Op.Binary {
  def get(col: Int, row: Int) = d2i(f(left.getDouble(col, row), right.getDouble(col, row)))
  def getDouble(col: Int, row: Int) = f(left.getDouble(col, row), right.getDouble(col, row))
}