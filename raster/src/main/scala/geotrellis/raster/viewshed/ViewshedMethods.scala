package geotrellis.raster.viewshed

import geotrellis.raster._
import geotrellis.util.MethodExtensions


trait ViewshedMethods extends MethodExtensions[Tile] {
  def viewshed(col: Int, row: Int, exact: Boolean = false): Tile =
    if (exact)
      Viewshed(self, col, row)
    else
      ApproxViewshed(self, col, row)

  def viewshedOffsets(col: Int, row: Int, exact: Boolean = false): Tile =
    if (exact)
      Viewshed.offsets(self, col, row)
    else
      ApproxViewshed.offsets(self, col, row)
}
