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

package geotrellis.vector.io.json

import geotrellis.vector._
import geotrellis.vector.io._

import org.scalatest._

class StyleSpec extends FunSpec with Matchers {
  describe("Styling geometries") {
    it("should style the geometry from a user defines style and read it back in") {
      val p = """{ "type": "Polygon", "coordinates": [ [ [-76.97021484375, 40.17887331434696], [-74.02587890625, 39.842286020743394],
	           [-73.4326171875, 41.713930073371294], [-76.79443359375, 41.94314874732696],
                   [-76.97021484375, 40.17887331434696] ] ] }""".parseGeoJson[Polygon]

      val geoJson =
        Feature(p, Style(strokeColor = "#555555", strokeWidth = "2", fillColor = "#00aa22", fillOpacity = 0.5)).toGeoJson

      val Feature(_, style) = geoJson.parseGeoJson[Feature[Polygon, Style]]
      style.strokeColor should be (Some("#555555"))
      style.strokeWidth should be (Some("2"))
      style.strokeOpacity should be (None)
      style.fillColor should be (Some("#00aa22"))
      style.fillOpacity should be (Some(0.5))

      println(geoJson)
    }
  }
}
