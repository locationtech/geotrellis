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

package geotrellis.spark.split

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.vector._
import geotrellis.spark.testkit._

import org.apache.spark.rdd.RDD
import org.scalatest._

class ProjectedExtentRDDSplitMethodsSpec extends FunSpec
    with Matchers
    with TestEnvironment
    with TileBuilders {
  describe("Splitting an RDD[(ProjectedExtent, Tile)]") {
    it("should split an example correctly") {
      val tile1 =
        createTile(
          Array( 1,1,1, 2,2,2, 3,3,
                 1,1,1, 2,2,2, 3,3,

                 4,4,4, 5,5,5, 6,6,
                 4,4,4, 5,5,5, 6,6 ),
          8, 4)

      val e1 = Extent(0.0, 0.0, 8.0, 10.0)

      val tile2 =
        createTile(
          Array( 1,1,
                 1,1 ),
          2, 2)

      val e2 = Extent(0.0, 0.0, 2.0, 2.0)

      val rdd: RDD[(ProjectedExtent, Tile)] =
        sc.parallelize(
          Seq(
            (ProjectedExtent(e1, LatLng), tile1),
            (ProjectedExtent(e2, LatLng), tile2)
          )
        )

      val results = rdd.split(tileCols = 3, tileRows = 2).collect()

      results.size should be (7)
    }
  }
}
