/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector

import geotrellis.vector.reproject._
import geotrellis.vector.io.json._
import geotrellis.proj4._
import geotrellis.testkit.vector._

import org.scalatest._

class ReprojectSpec extends FunSpec with Matchers {
  describe("reprojection") {
    it("should reproject a bounding box from WebMercator to LatLng and vica versa") {
      //-111.09374999999999,34.784483415461345,-75.322265625,43.29919735147067
      val ll = Line((-111.09374999999999,34.784483415461345),(-111.09374999999999,43.29919735147067),(-75.322265625,43.29919735147067),(-75.322265625,34.784483415461345),(-111.09374999999999,34.784483415461345))
      // -12366899.680315234,4134631.734001753,-8384836.254770693,5357624.186564572
      val wm = Line((-12366899.680315234,4134631.734001753),(-12366899.680315234,5357624.186564572),(-8384836.254770693,5357624.186564572),(-8384836.254770693,4134631.734001753),(-12366899.680315234,4134631.734001753))

      ll.reproject(LatLng, WebMercator) should matchGeom(wm, 0.00001)
      wm.reproject(WebMercator, LatLng) should matchGeom(ll, 0.00001)
    }
  }
}
