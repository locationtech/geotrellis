package geotrellis.raster.op.global

import geotrellis.raster._
import geotrellis.feature._

trait GlobalMethods extends TileMethods {
  def convolve(kernel: Kernel) =
    Convolve(tile, kernel)

  def costDistance(points: Seq[(Int, Int)]) = 
    CostDistance(tile, points)

  def toVector(extent: Extent): List[PolygonFeature[Int]] = 
    toVector(extent, RegionGroupOptions.default.connectivity)

  def toVector(extent: Extent, regionConnectivity: Connectivity): List[PolygonFeature[Int]] = 
    ToVector(tile, extent, regionConnectivity)

  def regionGroup(): RegionGroupResult =
    regionGroup(RegionGroupOptions.default)

  def regionGroup(options: RegionGroupOptions): RegionGroupResult =
    RegionGroup(tile, options)

  def verticalFlip(): Tile =
    VerticalFlip(tile)

  def viewshed(col: Int, row: Int, exact: Boolean = false): Tile =
    if(exact)
      Viewshed(tile, col, row)
    else
      ApproxViewshed(tile, col, row)

  def viewshedOffsets(col: Int, row: Int, exact: Boolean = false): Tile =
    if(exact)
      Viewshed.offsets(tile, col, row)
    else
      ApproxViewshed.offsets(tile, col, row)
}
