package geotrellis.geometry

import scala.math.{atan, exp, floor, log, sin}

package object projection {
  
  val earthRadius = 6378137.0

  /**
   * Given geographic coordinates (lng, lat), return web mercator
   * coordinates (x, y).
   */
  def latLngToWebMercator(lng:Double, lat:Double) = {
    // TODO: figure out what to call const
    val const = 0.017453292519943295

    val x = lng * const * earthRadius

    val a = lat * const
    val b = (1.0 + sin(a)) / (1.0 - sin(a))
    val y = (earthRadius / 2) * log(b)

    (x, y)
  }

  /**
   * Given web mercator coordinates (x, y), return geographic
   * coordinates (lng, lat)
   */
  def latLngToGeographic(x:Double, y:Double) = {
    val lowerLimit = -20037508.3427892
    val upperLimit =  20037508.3427892

    if (x < lowerLimit || x > upperLimit) {
      throw new Exception("point is outside mercator projection")
    }

    // TODO: figure out what to call these constants
    val const1 = 57.295779513082323
    val const2 = 1.5707963267948966

    // determine the latitude
    val a = x / earthRadius;
    val b = a * const1
    val c = floor((b + 180.0) / 360.0) * 360.0
    val lat = b - c

    // determine the longitude
    val d = const2 - (2.0 * atan(exp((-1.0 * y) / earthRadius)))
    val lng = d * const1

    (lng, lat)
  }
}
