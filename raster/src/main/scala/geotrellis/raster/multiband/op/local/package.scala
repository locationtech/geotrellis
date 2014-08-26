package geotrellis.raster.multiband.op

import geotrellis.raster.multiband._

package object local {
  implicit class LocalExtensions(val mTile: MultiBandTile) extends LocalMethods {}
}