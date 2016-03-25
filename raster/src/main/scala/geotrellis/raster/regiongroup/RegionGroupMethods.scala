package geotrellis.raster.regiongroup

import geotrellis.raster._
import geotrellis.util.MethodExtensions


/**
  * Trait for region group extension methods on [[Tile]].
  */
trait RegionGroupMethods extends MethodExtensions[Tile] {

  /**
    * Compute the region groupings for the present [[Tile]] using the
    * default options.
    */
  def regionGroup: RegionGroupResult =
    regionGroup(RegionGroupOptions.default)

  /**
    * Compute the region groupings for the present [[Tile]] using the
    * given options.
    */
  def regionGroup(options: RegionGroupOptions = RegionGroupOptions.default): RegionGroupResult =
    RegionGroup(self, options)
}
