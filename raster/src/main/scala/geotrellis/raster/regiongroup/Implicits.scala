package geotrellis.raster.regiongroup

import geotrellis.raster.Tile


object Implicits extends Implicits

trait Implicits {
  implicit class withSinglebandRegionGroupMethdos(val self: Tile) extends RegionGroupMethods
}
