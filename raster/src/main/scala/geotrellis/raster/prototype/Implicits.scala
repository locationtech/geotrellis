package geotrellis.raster.prototype

import geotrellis.raster._
import cats.Monoid

object Implicits extends Implicits

trait Implicits {
  implicit class withTileFeaturePrototypeMethods[
    T <: CellGrid : (? => TilePrototypeMethods[T]), 
    D: Monoid
  ](self: TileFeature[T, D]) extends TileFeaturePrototypeMethods[T, D](self)
}
