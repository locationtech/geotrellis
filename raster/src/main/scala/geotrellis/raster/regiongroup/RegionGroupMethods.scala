package geotrellis.raster.regiongroup

import geotrellis.raster._

trait RegionGroupMethods extends MethodExtensions[Tile] {
  def regionGroup: RegionGroupResult =
    regionGroup(RegionGroupOptions.default)

  def regionGroup(options: RegionGroupOptions = RegionGroupOptions.default): RegionGroupResult =
    RegionGroup(self, options)
}