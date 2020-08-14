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

package geotrellis.vector.reproject

import geotrellis.proj4._
import geotrellis.vector._

import geotrellis.vector.testkit._
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class ReprojectSpec extends AnyFunSpec with Matchers {
  describe("reprojection") {
    it("should reproject a bounding box from WebMercator to LatLng and vica versa") {
      //-111.09374999999999,34.784483415461345,-75.322265625,43.29919735147067
      val ll = LineString((-111.09374999999999,34.784483415461345),(-111.09374999999999,43.29919735147067),(-75.322265625,43.29919735147067),(-75.322265625,34.784483415461345),(-111.09374999999999,34.784483415461345))
      // -12366899.680315234,4134631.734001753,-8384836.254770693,5357624.186564572
      val wm = LineString((-12366899.680315234,4134631.734001753),(-12366899.680315234,5357624.186564572),(-8384836.254770693,5357624.186564572),(-8384836.254770693,4134631.734001753),(-12366899.680315234,4134631.734001753))

      ll.reproject(LatLng, WebMercator) should matchGeom(wm, 0.00001)
      wm.reproject(WebMercator, LatLng) should matchGeom(ll, 0.00001)
      ll.reproject(LatLng, Sinusoidal).reproject(Sinusoidal, WebMercator) should matchGeom(wm, 0.00001)
    }

    // see issue https://github.com/locationtech/geotrellis/issues/3023
    it("should not go into the infinite recursion stack") {
      val ext = Extent(0.0, 0.0, 2606.0, 2208.0)
      // we declare the extent as coords in LatLng, see issue #3023
      // though valid
      // - longitudes are in [-180, 180] degrees ragne
      // - latitudes are in [-90, 90] degrees range

      val transform = Transform(LatLng, WebMercator)
      // should not fail into the recursion stack
      intercept[IllegalArgumentException] {
        Reproject.reprojectExtentAsPolygon(ext, transform, 0.001)
      }
    }
  }
}
