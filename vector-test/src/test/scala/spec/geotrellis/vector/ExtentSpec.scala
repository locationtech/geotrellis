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

import geotrellis.vector.io._
import geotrellis.vector.io.json._
import org.scalatest.FunSpec
import org.scalatest.Matchers
import spray.json.DefaultJsonProtocol._

class ExtentSpec extends FunSpec with Matchers {
  describe("Extent") {
    it("should throw exceptions on invalid ranges") {
      intercept[ExtentRangeError] { Extent(10.0, 0.0, 0.0, 10.0) }
      intercept[ExtentRangeError] { Extent(0.0, 10.0, 10.0, 0.0) }
    }

    it("should compare extents") {
      val e1 = Extent(0.0, 0.0, 10.0, 10.0)
      val e2 = Extent(0.0, 20.0, 10.0, 30.0)
      val e3 = Extent(20.0, 0.0, 30.0, 10.0)
      val e4 = Extent(0.0, 0.0, 20.0, 20.0)
      val e5 = Extent(0.0, 0.0, 10.0, 30.0)

      assert((e1 compare e1) === 0)
      assert((e1 compare e2) === -1)
      assert((e1 compare e3) === -1)
      assert((e1 compare e4) === -1)
      assert((e1 compare e5) === -1)

      assert((e2 compare e1) === 1)
      assert((e2 compare e2) === 0)
      assert((e2 compare e3) === 1)
      assert((e2 compare e4) === 1)
      assert((e2 compare e5) === 1)

      assert((e3 compare e1) === 1)
      assert((e3 compare e2) === -1)
      assert((e3 compare e3) === 0)
      assert((e3 compare e4) === 1)
      assert((e3 compare e5) === 1)

      assert((e4 compare e1) === 1)
      assert((e4 compare e2) === -1)
      assert((e4 compare e3) === -1)
      assert((e4 compare e4) === 0)
      assert((e4 compare e5) === -1)

      assert((e5 compare e1) === 1)
      assert((e5 compare e2) === -1)
      assert((e5 compare e3) === -1)
      assert((e5 compare e4) === 1)
      assert((e5 compare e5) === 0)
    }

    it("should combine extents") {
      val e1 = Extent(0.0, 0.0, 10.0, 10.0)
      val e2 = Extent(20.0, 0.0, 30.0, 10.0)
      val e3 = Extent(0.0, 0.0, 30.0, 10.0)
      assert(e1.combine(e2) === e3)
    }

    it("should contains interior points") {
      val e = Extent(0.0, 0.0, 10.0, 10.0)
      assert(e.contains(3.0, 3.0) === true)
      assert(e.contains(0.00001, 9.9999) === true)
    }

    it("should not contain exterior points") {
      val e = Extent(0.0, 0.0, 10.0, 10.0)
      assert(e.contains(100.0, 0.0) === false)
      assert(e.contains(0.0, 1000.0) === false)
    }

    it("should not contain boundary") {
      val e = Extent(0.0, 0.0, 10.0, 10.0)
      assert(e.contains(0.0, 0.0) === false)
      assert(e.contains(0.0, 3.0) === false)
      assert(e.contains(0.0, 10.0) === false)
      assert(e.contains(10.0, 0.0) === false)
      assert(e.contains(10.0, 10.0) === false)
    }

    it("should get corners") {
      val e = Extent(0.0, 0.0, 10.0, 10.0)
      assert(e.southWest === Point(0.0, 0.0))
      assert(e.northEast === Point(10.0, 10.0))
    }

    it("should return valid results for contains against various extents") {
      val e = Extent(0.0, 100.0, 10.0, 200.0)
      assert(e.contains(e))
      assert(e.contains(Extent(1.0, 102.0, 9.0,170.0)))
      assert(!e.contains(Extent(-1.0, 102.0, 9.0,170.0)))
      assert(!e.contains(Extent(1.0, -102.0, 9.0,170.0)))
      assert(!e.contains(Extent(1.0, 102.0, 19.0,170.0)))
      assert(!e.contains(Extent(1.0, 102.0, 9.0,370.0)))
    }

    it("should return valid results for intersects") {
      val base = Extent(0.0, -20.0, 100.0, -10.0)

     def does(other: Extent): Unit =
       base.intersects(other) should be (true)

     def doesnot(other: Extent): Unit =
       base.intersects(other) should be (false)

      doesnot(Extent(-100.0,-20.0,-1.0,-10.0))
      does(Extent(-100.0,-20.0,0.0,-10.0))
      does(Extent(-100.0,-20.0,10.0,-10.0))
      does(Extent(0.0,-20.0,10.0,-10.0))
      does(Extent(40.0,-20.0,120.0,-10.0))
      does(Extent(100.0,-20.0,120.0,-10.0))
      doesnot(Extent(110.0,-20.0,120.0,-10.0))
      does(Extent(-100.0,-20.0,120.0,-10.0))
      doesnot(Extent(-100.0,-30.0,120.0,-21.0))
      does(Extent(-100.0,-30.0,120.0,-20.0))
      does(Extent(-100.0,-15.0,120.0,-10.0))
      does(Extent(-100.0,-15.0,120.0,0.0))
      doesnot(Extent(-100.0,-9.0,120.0,0.0))
      doesnot(Extent(-100.0,-9.0,-10.0,0.0))
    }

    it("should buffer") {
      Extent(0.0, -5.0, 10.0, 5.0).buffer(10) should be (
        Extent(-10.0, -15.0, 20.0, 15.0)
      )
    }

    it (" should give envelopes for geometries and features") {

      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val l2 = Line(Point(1,1), Point(1,6), Point(6,6), Point(6,1), Point(1,1))

      val p1: Polygon = Polygon(l1)
      val p2: Polygon = Polygon(l2)

      val env1 = Point(18.0,23.00).envelope
      assert(env1 === Extent(18.0,23.0,18.0,23.0))

      val env2 = l1.envelope
      assert(env2 === Extent(0.0,0.0,5.0,5.0))

      val env3 = p1.envelope
      assert(env3 === env2)

      val env4 = Seq(p1, p2).envelope
      assert(env4 === Extent(0.0,0.0,6.0,6.0))

      val json = Seq(p1, p2).toGeoJson
      val polygonsBack = json.parseGeoJson[GeometryCollection].polygons
      val env5 = polygonsBack.envelope
      assert(env5 === env4)

      case class SomeData(name: String, value: Double)
      implicit val someDataFormat = jsonFormat2(SomeData)

      val jsonFeature =
        """{
          |  "type": "Feature",
          |  "geometry": {
          |    "type": "Point",
          |    "coordinates": [18.00, 23.00]
          |  },
          |  "properties": {
          |    "name": "Bob",
          |    "value": 32.2
          |  }
          |}""".stripMargin
      val feature = PointFeature(Point(18.0,23.00), SomeData("Bob", 32.2))

      val env6 = feature.envelope
      assert(env6 === env1)
      val env7 = feature.geom.envelope
      assert(env7 === env6)

      case class DataBox(data: Int)
      implicit val boxFormat = jsonFormat1(DataBox)
      val jsonFc = """{
                     |  "type":"FeatureCollection",
                     |  "features":[
                     |    {"type":"Feature","geometry":{"type":"Point","coordinates":[10.34,22.75]},"properties":{"data" : 291},"id":"jackson5"},
                     |    {"type":"Feature","geometry":{"type":"Point","coordinates":[18.0,23.0]},"properties":{"data": 1273},"id":"volcano"},
                     |    {"type":"Feature","geometry":{"type":"Point","coordinates":[14.13,11.21]},"properties":{"data": 142},"id":"zorp"}
                     |  ]
                     |}""".stripMargin
      val env8 = jsonFc.parseGeoJson[JsonFeatureCollection].getAllPoints().envelope
      assert(env8.contains(env7))
    }
  }
}
