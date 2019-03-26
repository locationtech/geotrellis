package geotrellis.raster.costdistance

import geotrellis.raster.Tile


object Implicits extends Implicits

trait Implicits {
  implicit class withTileCostDistanceMethods(val self: Tile) extends CostDistanceMethods
}
