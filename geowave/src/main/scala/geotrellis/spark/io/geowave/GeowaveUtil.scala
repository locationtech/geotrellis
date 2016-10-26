package geotrellis.spark.io.geowave

import geotrellis.util.annotations.experimental

import scala.math.{ abs, pow, round }


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeowaveUtil {

  /**
    * $experimental If an edge or corner of an extent is very close to a split in
    * the GeoWave index (for some given number of bits), then it
    * should be snapped to the split to avoid pathological behavior).
    */
  @experimental def rectify(bits: Int)(_x: Double) = {
    val division = if (bits > 0) pow(2, -bits) ; else 1
    val x = (_x / 360.0) / division
    val xPrime = round(x)

    360 * division * (if (abs(x - xPrime) < 0.000001) xPrime ; else x)
  }
}
