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

package geotrellis.spark.summary.polygonal

import geotrellis.spark._
import geotrellis.spark.store.hadoop._
import geotrellis.spark.testkit.testfiles._
import geotrellis.raster.summary.polygonal._
import geotrellis.spark.testkit._

import geotrellis.vector._
import geotrellis.vector.summary.polygonal._

import org.scalatest._

class FeatureRDDSpec extends FunSpec
    with Matchers
    with TestEnvironment {

  describe("Zonal summary on an RDD of features") {
    it("should compute the area of features under a zone") {
      val lowerExtent = Extent(1, 1, 7, 3) // Partially intersects
      val middleExtent = Extent(3, 3, 5, 4) // Contained
      val upperExtent = Extent(1, 4, 7, 6) // Partially intersects

      val polygon = Polygon( (2, 2), (4, 6), (6, 2), (2, 2) )

      val featureRdd = sc.parallelize(Array(lowerExtent, middleExtent, upperExtent).map { e => Feature(e.toPolygon, e.area) })
      val result = featureRdd.polygonalSummary(polygon, 0.0)(
        PolygonalSummaryHandler({ feature: Feature[Polygon, Double] => feature.data })
                           ({ (polygon, feature) => polygon.intersection(feature.geom).asMultiPolygon.map(_.area).getOrElse(0.0) })
                           ({ (v1, v2) => v1 + v2 })
      )
      val expected = polygon.area - 0.5

      result should be (expected)
    }
  }
}
