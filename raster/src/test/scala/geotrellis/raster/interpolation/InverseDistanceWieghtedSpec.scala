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

package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.Circle
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json.JsonFeatureCollection
import geotrellis.raster.testkit._
import spray.json.DefaultJsonProtocol._
import org.scalatest._

class InverseDistanceWeightedSpec extends FunSpec
    with Matchers
    with RasterMatchers with TestFiles
    with TileBuilders {
  describe("interpolates integer values") {
    it("matches a QGIS generated IDW raster") {
      val rs = loadTestArg("data/schoolidw")
      val re = rs.rasterExtent
      val r = rs.tile

      val path = "raster/data/schoolgeo.json"

      val f = scala.io.Source.fromFile(path)
      val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
      f.close

      val points = collection.getAllPointFeatures[Int]

      val result = points.inverseDistanceWeighted(re)

      for(col <- 0 until re.cols) {
        for(row <- 0 until re.rows) {
          val actual = result.tile.get(col,row)
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
        PointFeature(Point(3,92), 13),
        PointFeature(Point(0,90), 10)
      )
      val result = points.inverseDistanceWeighted(re)

      assert(result.tile.get(0, 0) === value)
    }

    it("keeps sampled values closer than sample radius") {
      val re = RasterExtent(Extent(0,0,90,100),10,10,9,10)
      val sampleValue1 = 15
      val sampleValue2 = 14
      val points = Seq(
        PointFeature(Point(5,95), sampleValue1),
        PointFeature(Point(5,92), sampleValue2),
        PointFeature(Point(0,90), 10)
      )
      val result = points.inverseDistanceWeighted(re, InverseDistanceWeighted.Options(equalWeightRadius = 3, onSet = x => Math.round(x)))

      assert(result.tile.get(0, 0) === Math.round((sampleValue1+sampleValue2)/2.0))
    }

    it("uses points closer than radius in raster units") {
      val re = RasterExtent(Extent(0,0,0.5,0.5),0.05,0.05,10,10)

      val points = Seq(
        PointFeature(Point(0.075,0.475), 10),
        PointFeature(Point(0.025,0.425), 20),
        PointFeature(Point(0.175,0.475), 500)
      )

      val result = points.inverseDistanceWeighted(re, InverseDistanceWeighted.Options(radiusX = 0.05, radiusY = 0.05))

      assert(result.tile.get(0, 0) === 15)
      assert(result.tile.get(3, 0) === 500)
    }
  }

  describe("interpolates double values") {
    it("keeps sampled values") {
      val re = RasterExtent(Extent(0,0,90,100),10,10,9,10)
      val value = 15.5
      val points = Seq(
        PointFeature[Double](Point(5,95), value),
        PointFeature[Double](Point(0,90), 10)
      )
      val result = points.inverseDistanceWeighted(re, InverseDistanceWeighted.Options(cellType = DoubleConstantNoDataCellType))

      value should be (result.tile.getDouble(0, 0) +- 0.001)
    }

    it("keeps sampled values closer than sample radius") {
      val re = RasterExtent(Extent(0,0,90,100),10,10,9,10)
      val sampleValue1 = 15
      val sampleValue2 = 14
      val points = Seq(
        PointFeature(Point(5,95), sampleValue1),
        PointFeature(Point(5,92), sampleValue2),
        PointFeature(Point(0,90), 10)
      )
      val result = points.inverseDistanceWeighted(re, InverseDistanceWeighted.Options(equalWeightRadius = 3, cellType = DoubleConstantNoDataCellType))

      assert(result.tile.getDouble(0, 0) === (sampleValue1+sampleValue2)/2.0)
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

      val result = points.inverseDistanceWeighted(re, InverseDistanceWeighted.Options(radiusX = 0.05, radiusY = 0.05, cellType = DoubleConstantNoDataCellType))

      expected should be (result.tile.getDouble(0, 0) +- 0.001)
      c should be (result.tile.getDouble(3, 0) +- 0.001)
    }
  }
}
