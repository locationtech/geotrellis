package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.IfCell
import org.apache.spark.Partitioner

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
    trueValue: Int): Self = localIf(other, cond, trueValue, None)

  def localIf(
    other: Self,
    cond: (Int, Int) => Boolean,
    trueValue: Int,
    partitioner: Option[Partitioner]
  ): Self = self.combineValues(other, partitioner) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue)
  }

  def localIf(
    other: Self,
    cond: (Double, Double) => Boolean,
    trueValue: Double): Self = localIf(other, cond, trueValue, None)

  def localIf(
    other: Self,
    cond: (Double, Double) => Boolean,
    trueValue: Double,
    partitioner: Option[Partitioner]
  ): Self = self.combineValues(other, partitioner) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue)
  }

  def localIf(
    other: Self,
    cond: (Int, Int) => Boolean,
    trueValue: Int,
    falseValue: Int): Self = localIf(other, cond, trueValue, falseValue, None)

  def localIf(
    other: Self,
    cond: (Int, Int) => Boolean,
    trueValue: Int,
    falseValue: Int,
    partitioner: Option[Partitioner]
  ): Self = self.combineValues(other, partitioner) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue, falseValue)
  }

  def localIf(
    other: Self,
    cond: (Double, Double) => Boolean,
    trueValue: Double,
    falseValue: Double): Self = localIf(other, cond, trueValue, falseValue, None)

  def localIf(
    other: Self,
    cond: (Double, Double) => Boolean,
    trueValue: Double,
    falseValue: Double,
    partitioner: Option[Partitioner]
  ): Self = self.combineValues(other, partitioner) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue, falseValue)
  }
}
