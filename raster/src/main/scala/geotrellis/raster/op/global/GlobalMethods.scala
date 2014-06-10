package geotrellis.raster.op.global

import geotrellis.raster._
import geotrellis.feature.Extent

trait GlobalMethods extends TileMethods {
  def convolve(kernel: Kernel) =
    Convolve(tile, kernel)

  def costDistance(points: Seq[(Int, Int)]) = 
    CostDistance(tile, points)

  def toVector(extent: Extent, regionConnectivity: Connectivity = RegionGroupOptions.default.connectivity) = 
    ToVector(tile, extent, regionConnectivity)

  def regionGroup(options: RegionGroupOptions = RegionGroupOptions.default) =
    RegionGroup(tile, options)

  def verticalFlip() =
    VerticalFlip(tile)

  def viewshed(col: Int, row: Int, exact: Boolean = false) =
    if(exact)
      Viewshed(tile, col, row)
    else
      ApproxViewshed(tile, col, row)

  def viewshedOffsets(col: Int, row: Int, exact: Boolean = false) =
    if(exact)
      Viewshed.offsets(tile, col, row)
    else
      ApproxViewshed.offsets(tile, col, row)
}
