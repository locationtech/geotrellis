package geotrellis.raster

import geotrellis.raster.histogram._

package object render {
  implicit class MultiBandRenderMethodExtensions(val tile: MultiBandTile) extends MultiBandPngRenderMethods { }
}
