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

import geotrellis.vector._

import org.scalatest._

class ShapeFileReaderSpec extends FunSpec with Matchers {
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
}
