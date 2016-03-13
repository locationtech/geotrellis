package geotrellis.raster

import geotrellis.raster.histogram._

package object render {
  implicit class MultibandRenderMethodExtensions(val tile: MultibandTile) extends MultibandPngRenderMethods { }
}
