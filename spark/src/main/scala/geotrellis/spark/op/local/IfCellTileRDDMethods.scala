package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.IfCell

trait IfCellTileRDDMethods[K] extends TileRDDMethods[K] {

  def localIf(cond: Int => Boolean, trueValue: Int) =
    self.mapValues { r => IfCell(r, cond, trueValue) }

  def localIf(
    cond: Double => Boolean,
    trueValue: Double
  ) = self.mapValues { r => IfCell(r, cond, trueValue) }

  def localIf(
    cond: Int => Boolean,
    trueValue: Int,
    falseValue: Int
  ) = self.mapValues {
    r => IfCell(r, cond, trueValue, falseValue)
  }

  def localIf(
    cond: Double => Boolean,
    trueValue: Double,
    falseValue: Double
  ) = self.mapValues {
    r => IfCell(r, cond, trueValue, falseValue)
  }

  def localIf(
    other: Self,
    cond: (Int, Int) => Boolean,
    trueValue: Int
  ) = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue)
  }

  def localIf(
    other: Self,
    cond: (Double, Double) => Boolean,
    trueValue: Double
  ) = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue)
  }

  def localIf(
    other: Self,
    cond: (Int, Int) => Boolean,
    trueValue: Int,
    falseValue: Int
  ) = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue, falseValue)
  }

  def localIf(
    other: Self,
    cond: (Double, Double) => Boolean,
    trueValue: Double,
    falseValue: Double
  ) = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue, falseValue)
  }
}
