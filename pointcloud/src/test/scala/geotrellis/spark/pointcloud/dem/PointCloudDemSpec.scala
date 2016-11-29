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

package geotrellis.spark.pointcloud.dem

import geotrellis.raster.TileLayout
import geotrellis.spark.PointCloudTestEnvironment
import Implicits._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.spark.io.hadoop.HadoopPointCloudRDD
import geotrellis.vector.Extent

import org.scalatest._


class PointCloudDemSpec extends FunSpec
  with Matchers
  with PointCloudTestEnvironment {

  describe("PointCloud DEM support") {

    it("should be able to union two clouds") {
      val cloud = HadoopPointCloudRDD(lasPath).first._2
      val clouds = cloud.union(cloud)

      clouds.length should be (cloud.length * 2)
    }
  }
}
