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

package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.io._
import geotrellis.raster._
import geotrellis.feature._
import geotrellis.io._

import org.scalatest._

import geotrellis.testkit._

import com.vividsolutions.jts.{ geom => jts }
import geotrellis.feature.json._

class IDWInterpolateSpec extends FunSpec 
                            with Matchers 
                            with TestServer 
                            with RasterBuilders {
  describe("IDWInterpolate") {
    it("matches a QGIS generated IDW raster") {
      val r = get(LoadRaster("schoolidw"))
      val re = r.rasterExtent

      val path = "core-test/data/schoolgeo.json"

      val f = scala.io.Source.fromFile(path)
      val collection = f.mkString.parseGeoJson[JsonFeatureCollection]

      f.close

      val points = collection.getAllPoints[Int]

      val result = VectorToRaster.idwInterpolate(points, re).get
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
}
