package geotrellis.layers.mapalgebra.local

import geotrellis.raster.mapalgebra.local.IfCell
import geotrellis.raster.Tile
import geotrellis.layers._
import geotrellis.util.MethodExtensions


trait IfCellTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {

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
    other: Seq[(K, Tile)],
    cond: (Int, Int) => Boolean,
    trueValue: Int
  ): Seq[(K, Tile)] = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue)
  }

  def localIf(
    other: Seq[(K, Tile)],
    cond: (Double, Double) => Boolean,
    trueValue: Double
  ): Seq[(K, Tile)] = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue)
  }

  def localIf(
    other: Seq[(K, Tile)],
    cond: (Int, Int) => Boolean,
    trueValue: Int,
    falseValue: Int
  ): Seq[(K, Tile)] = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue, falseValue)
  }

  def localIf(
    other: Seq[(K, Tile)],
    cond: (Double, Double) => Boolean,
    trueValue: Double,
    falseValue: Double
  ): Seq[(K, Tile)] = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue, falseValue)
  }
}
