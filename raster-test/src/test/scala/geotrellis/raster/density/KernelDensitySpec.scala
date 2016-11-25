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

package geotrellis.raster.density

import geotrellis.raster._
import geotrellis.raster.density._
import geotrellis.raster.testkit._
import geotrellis.vector._

import org.scalatest._

class KernelDensitySpec extends FunSpec
                        with Matchers
                        with RasterMatchers with TestFiles
                        with TileBuilders
{
  describe("kernelDensity") {
    it("matches expected values") {
      val rasterExtent = RasterExtent(Extent(0,0,5,5),1,1,5,5)
      val n = NODATA
      val arr = Array(2,2,1,n,n,
        2,3,2,1,n,
        1,2,2,1,n,
        n,1,1,2,1,
        n,n,n,1,1)

      val tile = ArrayTile(arr,5, 5)

      val kernel =
        ArrayTile(
          Array(
            1,1,1,
            1,1,1,
            1,1,1),
          3,3)

      val points = Seq(
        PointFeature(Point(0,4.5),1),
        PointFeature(Point(1,3.5),1),
        PointFeature(Point(2,2.5),1),
        PointFeature(Point(4,0.5),1)
      )
      val result =
        points.kernelDensity(kernel, rasterExtent)

      assertEqual(result, tile)
    }
  }
}
