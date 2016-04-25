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

package geotrellis.raster

import geotrellis.raster.mapalgebra.focal.Circle
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json.JsonFeatureCollection
import geotrellis.raster.testkit._
import spray.json.DefaultJsonProtocol._
import org.scalatest._

class VectorToRasterSpec extends FunSpec
                            with Matchers
                            with RasterMatchers with TestFiles
                            with TileBuilders {
  describe("idwInterpolate") {
    it("matches a QGIS generated IDW raster") {
      val rs = loadTestArg("data/schoolidw")
      val re = rs.rasterExtent
      val r = rs.tile

      val path = "raster-test/data/schoolgeo.json"

      val f = scala.io.Source.fromFile(path)
      val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
      f.close

      val points = collection.getAllPointFeatures[Int]

      val result = VectorToRaster.idwInterpolate(points, re)
      var count = 0
      for(col <- 0 until re.cols) {
        for(row <- 0 until re.rows) {
          val actual = result.get(col,row)
          val expected = r.get(col,row)

          actual should be (expected +- 1)
        }
      }
    }

    it("keeps sampled values") {
      val re = RasterExtent(Extent(0,0,90,100),10,10,9,10)
      val value = 15
      val points = Seq(
        PointFeature(Point(5,95), value),
        PointFeature(Point(0,90), 10)
      )
      val result = VectorToRaster.idwInterpolate(points,re)

      assert(result.get(0, 0) === value)
    }

    it("uses points closer than radius in cell units") {
      val re = RasterExtent(Extent(0,0,0.5,0.5),0.05,0.05,10,10)

      val points = Seq(
        PointFeature(Point(0.075,0.475), 10),
        PointFeature(Point(0.025,0.425), 20),
        PointFeature(Point(0.175,0.475), 500)
      )

      val result = VectorToRaster.idwInterpolate(points,re, Circle(1))

      assert(result.get(0, 0) === 15)
      assert(result.get(3, 0) === 500)
    }

    it("uses points closer than radius in raster units") {
      val re = RasterExtent(Extent(0,0,0.5,0.5),0.05,0.05,10,10)

      val points = Seq(
        PointFeature(Point(0.075,0.475), 10),
        PointFeature(Point(0.025,0.425), 20),
        PointFeature(Point(0.175,0.475), 500)
      )

      val result = VectorToRaster.idwInterpolate(points,re, 0.05)

      assert(result.get(0, 0) === 15)
      assert(result.get(3, 0) === 500)
    }
  }

  describe("idwInterpolateDouble") {
    it("keeps sampled values") {
      val re = RasterExtent(Extent(0,0,90,100),10,10,9,10)
      val value = 15.5
      val points = Seq(
        PointFeature[Double](Point(5,95), value),
        PointFeature[Double](Point(0,90), 10)
      )
      val result = VectorToRaster.idwInterpolateDouble(points,re)

      value should be (result.getDouble(0, 0) +- 0.001)
    }

    it("uses points closer than radius in cell units") {
      val re = RasterExtent(Extent(0,0,0.5,0.5),0.05,0.05,10,10)

      val a = 10.2
      val b = 20.8
      val c = 500.0
      val expected = (a + b) / 2

      val points = Seq(
        PointFeature[Double](Point(0.075,0.475), a),
        PointFeature[Double](Point(0.025,0.425), b),
        PointFeature[Double](Point(0.175,0.475), c)
      )

      val result = VectorToRaster.idwInterpolateDouble(points,re, Circle(1))

      expected should be (result.getDouble(0, 0) +- 0.001)
      c should be (result.getDouble(3, 0) +- 0.001)
    }

    it("uses points closer than radius in raster units") {
      val re = RasterExtent(Extent(0,0,0.5,0.5),0.05,0.05,10,10)

      val a = 10.2
      val b = 20.8
      val c = 500.0
      val expected = (a + b) / 2

      val points = Seq(
        PointFeature[Double](Point(0.075,0.475), a),
        PointFeature[Double](Point(0.025,0.425), b),
        PointFeature[Double](Point(0.175,0.475), c)
      )

      val result = VectorToRaster.idwInterpolateDouble(points,re, 0.05)

      expected should be (result.getDouble(0, 0) +- 0.001)
      c should be (result.getDouble(3, 0) +- 0.001)
    }
  }

  describe("CountPoints") {
    it("returns a zero raster when empty points") {
      val re = RasterExtent(Extent(0,0,9,10),1,1,9,10)
      val result = VectorToRaster.countPoints(Seq[Point](),re)
      assertEqual(result, Array.fill[Int](90)(0))
    }

    it("should return 0 raster if points lie outside extent") {
      val re = RasterExtent(Extent(0,0,9,10),1,1,9,10)
      val points =
        Seq(
          Point(100,200),
          Point(-10,-30),
          Point(-310,1200)
        )
      val result = VectorToRaster.countPoints(points,re)
      assertEqual(result, Array.fill[Int](90)(0))
    }

    it("counts the points when they are all bunched up in one cell") {
      val re = RasterExtent(Extent(0,0,90,100),10,10,9,10)
      val points = Seq(
        Point(41,59),
        Point(42,58),
        Point(43,57),
        Point(44,56),
        Point(45,58)
      )
      val result = VectorToRaster.countPoints(points,re)

      val expected = Array.fill[Int](90)(0)
      expected(4*9 + 4) = 5

      assertEqual(result, expected)
    }

    it("gets counts in the right cells for multiple values") {
      val re = RasterExtent(Extent(0,0,90,100),10,10,9,10)
      val points = for(i <- 0 to 8) yield {
        Point(
          10*i + 1, /* ith col */
          100 - ((90 - 10*i) - 1) /* (10-i)'th col */
        )
      }

      val result = VectorToRaster.countPoints(points,re)
      val tile = IntArrayTile.ofDim(9,10)
      for(i <- 0 to 8) {
        tile.set(i,10 - (i+2),1)
      }

      assertEqual(result, tile)
    }
  }

  describe("kernelDensity") {
    it("matches expected values") {
      val rasterExtent = RasterExtent(Extent(0,0,5,5),1,1,5,5)
      val n = NODATA
      val arr = Array(2,2,1,n,n,
        2,3,2,1,n,
        1,2,2,1,n,
        n,1,1,2,1,
        n,n,n,1,1)

      val tile = ArrayTile(arr,5, 5)

      val kernel =
        ArrayTile(
          Array(
            1,1,1,
            1,1,1,
            1,1,1),
          3,3)

      val points = Seq(
        PointFeature(Point(0,4.5),1),
        PointFeature(Point(1,3.5),1),
        PointFeature(Point(2,2.5),1),
        PointFeature(Point(4,0.5),1)
      )
      val result =
        VectorToRaster.kernelDensity(points, kernel, rasterExtent)

      assertEqual(result, tile)
    }
  }
}
