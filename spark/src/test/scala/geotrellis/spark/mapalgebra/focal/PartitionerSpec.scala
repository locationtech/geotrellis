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

package geotrellis.spark.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.spark._
import geotrellis.spark.testkit._
import geotrellis.raster.io.geotiff._

import org.apache.spark._

import org.scalatest.FunSpec
import java.io._

class PartitionerSpec extends FunSpec with TestEnvironment {

  val tile = SinglebandGeoTiff(new File(inputHomeLocalPath, "aspect.tif").getPath).tile
  val (_, rasterRDD) = createTileLayerRDD(tile, 4, 3)

  describe("Focal Partitioner Spec") {

    it("should retain the partitioner of the parent RDD") {
      val partitionedParent = rasterRDD.withContext { _.partitionBy(new HashPartitioner(10)) }
      val childRDD = partitionedParent.slope().focalMin(Square(1))

      assert(childRDD.partitioner == partitionedParent.partitioner)
    }

    it("should retain its new partitioner") {
      val targetPartitioner = Some(new HashPartitioner(10))
      val minRDD = rasterRDD.slope(partitioner = targetPartitioner).focalMin(Square(1))

      assert(minRDD.partitioner == targetPartitioner)
    }
  }
}
