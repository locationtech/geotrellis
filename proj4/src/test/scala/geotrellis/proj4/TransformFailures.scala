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

package geotrellis.proj4

import org.osgeo.proj4j._

import org.scalatest._

/**
 * A class to run tests which are known to be failures.
 * This prevents Maven from running them automatically and reporting failures.
 * @author mdavis
 *
 */
class TransformFailures extends FunSuite with BaseCoordinateTransformTest {
  ignore("EPSG_27700") {
    checkTransform("EPSG:4326", -2.89, 55.4,    "EPSG:27700", 343733.1404, 612144.530677, 0.1 )
    checkTransformAndInverse(
                             "EPSG:4326", -2.0301713578021983, 53.35168607080468,
                             "EPSG:27700", 398089, 383867,
                             0.001, 0.2 * APPROX_METRE_IN_DEGREES)
  }
}
