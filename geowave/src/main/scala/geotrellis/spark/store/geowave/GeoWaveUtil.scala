/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.store.geowave

import geotrellis.util.annotations.experimental

import scala.math.{ abs, pow, round }


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeoWaveUtil {

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
