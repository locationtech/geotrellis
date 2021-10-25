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

package geotrellis.raster.resample

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
  * Since cubic spline resample inherits from cubic resample it
  * is only needed to test the actual math, the rest is tested in the bicubic
  * resample spec, the cubic resample spec and the bilinear
  * resample spec.
  *
  * It is also known that the bicubic resample resolves, if the cube
  * is D * D points, first D rows then the D values of each resample
  * result, and resamples them. So only one dimensional resample
  * is needed to be tested.
  *
  * The bicubic spline resample class uses a helper class,
  * which is the only class tested here.
  */
class BicubicSplineResampleSpec extends AnyFunSuite with Matchers {

  val E = 1e-2

  val ip = new CubicSplineResample()

  /*
   * These test cases are derived from using a MATLAB code snippet called
   * cakima.m found here:
   * http://www.mathworks.com/matlabcentral/fileexchange/36800-interpolation-utilities/content//cakima.m
   *
   * The only difference is that the MATLAB function uses a 1.XX prefix and we a
   * 0.XX prefix for the x parameter.
   */
  test("one dimensional cubic akima splines should work as expected") {
    ip.resample(Array(0, 0, 0, 0, 0, 0), 0) should be (0.0 +- E)
    ip.resample(Array(0, 1, 2, 3, 4, 5), 0.5) should be (2.5 +- E)
    ip.resample(Array(6, 8, 3, 4, 9, 5), 0.5) should be (3.02727 +- E)
    ip.resample(Array(15, 32, 10, 54, 22, 11), 0.33) should be (22.2643 +- E)
    ip.resample(Array(20, 40, 60, 80, 60, 20), 0.74) should be (74.8 +- E)
    ip.resample(Array(-12, 23, -5, -1, 0, 32), 0.45) should be (-3.2283 +- E)
    ip.resample(Array(65.5, 15.5, 45.1, 22.2, 11, 5), 0.89) should be (23.8639 +- E)
    ip.resample(Array(1, 2, 4, 8, 16, 32), 0.61) should be (6.1755 +- E)
    ip.resample(Array(-10, 15, 25, 25, 64, 128), 0.22) should be (25.546 +- E)
    ip.resample(Array(25, 20, 15, 10, 5, 0), 0.333) should be (13.3350 +- E)
  }

}
