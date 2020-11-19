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

package geotrellis.vector

import geotrellis.proj4.{LatLng, WebMercator}

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class GeometryCollectionSpec extends AnyFunSpec with Matchers {
  describe("GeometryCollection") {
    it("should reproject without duplication") {
      val p = Point(1,1)
      val mp = MultiPoint(p, p)
      val gc = GeometryCollection(multiPoints = Seq(mp))

      val pp = p.reproject(LatLng, WebMercator)
      val mpp = MultiPoint(pp, pp)
      val gcp = GeometryCollection(multiPoints = Seq(mpp))

      gc.reproject(LatLng, WebMercator) should be (gcp)
    }

    it("getAll should work right for all types") {
      val p0 = Point(0, 0)
      val p1 = Point(1, 0)
      val p2 = Point(0, 1)
      val ls = LineString(p0, p1)
      val pg = Polygon(p0, p1, p2, p0)
      val mp = MultiPoint(p0, p1, p2)
      val mpg = MultiPolygon(pg)
      val mls = MultiLineString(ls)
      val gc = GeometryCollection(points = Seq(p0))

      val gc1 = GeometryCollection(points = Seq(p0),
                                   lines = Seq(ls),
                                   polygons = Seq(pg),
                                   multiPoints = Seq(mp),
                                   multiLines = Seq(mls),
                                   multiPolygons = Seq(mpg),
                                   geometryCollections = Seq(gc))

      gc1.getAll[Point] should be (Seq(p0))
      gc1.getAll[LineString] should be (Seq(ls))
      gc1.getAll[Polygon] should be (Seq(pg))
      gc1.getAll[MultiPoint] should be (Seq(mp))
      gc1.getAll[MultiLineString] should be (Seq(mls))
      gc1.getAll[MultiPolygon] should be (Seq(mpg))
      gc1.getAll[GeometryCollection] should be (Seq(gc))
    }
  }
}
