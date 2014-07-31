package geotrellis.raster.op

import geotrellis.raster._

package object hydrology {
  implicit class HydrologyExtensions(val tile: Tile) extends HydrologyMethods
}
