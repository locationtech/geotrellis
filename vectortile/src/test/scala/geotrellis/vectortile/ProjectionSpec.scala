/*
 * Copyright 2020 Azavea
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

package geotrellis.vectortile

import geotrellis.proj4._
import geotrellis.vector._

import org.scalatest.funspec.AnyFunSpec

class ProjectionSpec extends AnyFunSpec {
  describe("VectorTile Projection Conversions") {
    it("should read a point from a VectorTile and write it back") {
      val p1 = Point(-61.347656249999986, 10.412183158667512)
      val reprojected = p1.reproject(LatLng, WebMercator)
      val tileExtent = Extent(-6887893.4928338025, 1095801.2374962866, -6809621.975869782, 1174072.7544603087)

      val f = MVTFeature(reprojected, Map.empty[String, Value])

      val layer = StrictLayer(
        name = "test",
        tileWidth = 128,
        version = 2,
        tileExtent = tileExtent,
        points = Seq(f),
        multiPoints = Seq.empty,
        lines = Seq.empty,
        multiLines = Seq.empty,
        polygons = Seq.empty,
        multiPolygons = Seq.empty
      )

      val vt = VectorTile(Map("test" -> layer), tileExtent)

      val vt2 = VectorTile.fromBytes(vt.toBytes, tileExtent)
      val p2 = vt2.layers("test").features.head.geom.reproject(WebMercator, LatLng)

      val vt3 = VectorTile.fromBytes(vt2.toBytes, tileExtent)
      val p3 = vt3.layers("test").features.head.geom.reproject(WebMercator, LatLng)

      assert((p1 == p2) && (p1 == p3) && (p2 == p3))
    }
  }
}
