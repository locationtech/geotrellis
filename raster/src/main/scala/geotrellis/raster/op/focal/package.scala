package geotrellis.raster.op

import geotrellis.raster._

package object focal {
  implicit class FocalExtensions(val tile: Tile) extends FocalMethods { }

  implicit class HillshadeTuple(val tuple: Tuple2[Tile, Tile]) {
    def hillshade(azimuth: Double, altitude: Double) = {
      val (aspect, slope) = tuple
      Hillshade.indirect(aspect, slope, azimuth, altitude)
    }
  }
}
