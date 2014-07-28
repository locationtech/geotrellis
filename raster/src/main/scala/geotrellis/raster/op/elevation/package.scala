package geotrellis.raster.op

import geotrellis.raster._

package object elevation {
  implicit class ElevationExtensions(val tile: Tile) extends ElevationMethods

  implicit class HillshadeTuple(val tuple: Tuple2[Tile, Tile]) {
    def hillshade(azimuth: Double, altitude: Double) = {
      val (aspect, slope) = tuple
      Hillshade.indirect(aspect, slope, azimuth, altitude)
    }
  }
}
