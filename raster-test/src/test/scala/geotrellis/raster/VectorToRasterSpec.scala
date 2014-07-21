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

import geotrellis.feature._
import geotrellis.feature.json._
import geotrellis.testkit._
import spray.json.DefaultJsonProtocol._

import spray.json.DefaultJsonProtocol._

import org.scalatest._

class VectorToRasterSpec extends FunSpec 
                            with Matchers 
                            with TestEngine 
                            with TileBuilders {
  describe("idwInterpolate") {
    it("matches a QGIS generated IDW raster") {
      val rs = RasterSource("schoolidw")
      val re = rs.rasterExtent.get
      val r = rs.get

      val path = "raster-test/data/schoolgeo.json"

      val f = scala.io.Source.fromFile(path)
      val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
      f.close

      val points = collection.getAllPoints[Int]

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
