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
import geotrellis.raster._
import geotrellis.feature._
import geotrellis.testkit._

import org.scalatest.FunSuite

class KernelDensitySpec extends FunSuite with TestServer {
  test("kernel density") {
    val rasterExtent = RasterExtent(Extent(0,0,5,5),1,1,5,5)
    val n = NODATA
    val arr = Array(2,2,1,n,n,
                    2,3,2,1,n,
                    1,2,2,1,n,
                    n,1,1,2,1,
                    n,n,n,1,1)
    val r = Raster(arr,rasterExtent)

    val kernel = Raster(Array(1,1,1,
                              1,1,1,
                              1,1,1),RasterExtent(Extent(0,0,3,3),1,1,3,3))

    val points = Seq(
      PointFeature(Point(0,4.5),1),
      PointFeature(Point(1,3.5),1),
      PointFeature(Point(2,2.5),1),
      PointFeature(Point(4,0.5),1)
    )
    val source = 
      VectorToRaster.kernelDensity(points, kernel, rasterExtent)

    assertEqual(source.get, r)
  }
}
