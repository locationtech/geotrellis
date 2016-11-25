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

package geotrellis.spark.points.tiling

import geotrellis.raster.TileLayout
import geotrellis.spark.PointsTestEnvironment
import geotrellis.spark.points.tiling.Implicits._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.spark.io.hadoop.HadoopPackedPointsRDD
import geotrellis.vector.Extent

import org.scalatest._

class PackedPointsTilingSpec extends FunSpec
  with Matchers
  with PointsTestEnvironment {
  describe("Points RDD tiling") {
    it("should tile RDD of packed points") {
      //Pipeline.loadNativeLibrary()
      val source = HadoopPackedPointsRDD(lasPath)
      val original = source.take(1).map(_._2).toList.head
      // that means there can be no more points per "tile" than tileCols * tileRows
      val ld = LayoutDefinition(
        Extent(635609.85, 848889.7, 638992.55, 853545.43),
        TileLayout(layoutCols = 5, layoutRows = 5, tileCols = 10, tileRows = 10)
      )
      val tiled = withTilerMethods(source).tileToLayout(ld)
      tiled.map(_._2.length).reduce(_ + _) should be (original.length)
      tiled.count() should be (25)
    }
  }
}
