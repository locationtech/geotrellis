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

package geotrellis.shapefile

import java.net.URL
import java.nio.charset.Charset

import geotrellis.vector._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class ShapeFileReaderSpec extends AnyFunSpec with Matchers {
  describe("ShapeFileReader") {
    it("should read multipolygons feature attribute") {
      val path = "shapefile/data/shapefiles/demographics/demographics.shp"
      val features = ShapeFileReader.readMultiPolygonFeatures(path)
      features.size should be (160)
      for(MultiPolygonFeature(polygon: MultiPolygon, data: Map[String, Object]) <- features) {
        data.keys.toSeq should be (Seq("LowIncome", "gbcode", "ename", "WorkingAge", "TotalPop", "Employment"))
      }
    }

  }

  describe("Issue 3445") {
    it("should read SimpleFeatures with different charSet") {
      val url = new URL("https://github.com/locationtech/geotrellis/raw/master/shapefile/data/shapefiles/demographics/demographics.shp")
      val features = ShapefileReader.readSimpleFeatures(url)
      features.size should be (160)

      val features1 = ShapefileReader.readSimpleFeatures(url, Charset.forName("UTF-8"))
      features1.size should be (160)
    }
  }
}
