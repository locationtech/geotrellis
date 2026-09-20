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

package geotrellis.util.conversions

import spire.math.Integral

/**
 * spire ships the coercion functions (`ConvertableTo[A].fromInt` and friends) but no
 * `scala.Conversion` instances, so on Scala 3 an `Int` no longer widens to an `N: Integral`
 * the way it did on 2.13. This supplies that conversion.
 */
object ConversionLiftSpire {
  implicit def widenIntToIntegral[N](implicit ev: Integral[N]): Conversion[Int, N] =
    new Conversion[Int, N] { def apply(i: Int): N = ev.fromInt(i) }
}
