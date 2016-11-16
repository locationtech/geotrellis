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

package geotrellis.raster.dem

import geotrellis.raster._
import geotrellis.vector._

import org.scalatest._


class PointCloudSpec extends FunSpec with Matchers {

  val re = RasterExtent(Extent(0, 0, 10, 10), 10, 10)
  val points = Array(Point(0.5, 0.5), Point(0.5, 9.5), Point(9.5, 9.5))
  val zs = Array(0, 1000, 2000)

  describe("The PointCloud class") {
    it("should correctly interpolate at triangle vertices") {
      val cloud = PointCloud(points, zs)
      val tile = cloud.toTile(re, Z)
      val array = tile.toArrayDouble

      array(0) should be (0)
      array(9) should be (1000)
      array(99) should be (2000)
    }

    it("should correctly interpolate all pixels (1/2)") {
      val cloud = PointCloud(points, zs)
      val tile = cloud.toTile(re, Z)
      val array = tile.toArrayDouble

      array.filter(!java.lang.Double.isNaN(_)).length should be (55)
    }

    it("should correctly interpolate all pixels (2/2)") {
      val cloud = PointCloud(
        points ++ Array(Point(9.5, 0.5)),
        zs ++ Array(3000)
      )
      val tile = cloud.toTile(re, Z)
      val array = tile.toArrayDouble

      array.filter(!java.lang.Double.isNaN(_)).length should be (100)
    }

  }

}
