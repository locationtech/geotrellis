/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.gdal

import com.azavea.gdal.GDALWarp
import geotrellis.raster.testkit.Resource

import org.scalatest.funspec.AnyFunSpec

class GDALErrorSpec extends AnyFunSpec {
  val uri = Resource.path("vlm/c41078a1.tif")
  val token = GDALWarp.get_token(uri, Array())
  val dataset: GDALDataset.DatasetType = GDALWarpOptions.EMPTY.datasetType

  val sourceWindow: Array[Int] = Array(1000000, 1000000, 5000000, 5000000)
  val destWindow: Array[Int] = Array(500, 500)
  val buffer = Array.ofDim[Byte](500 * 500)

  describe("GDALErrors") {
    it("should return the ObjectNull error code") {
      val result = GDALWarp.get_data(token, dataset.value, 1, sourceWindow, destWindow, 42, 1, buffer)

      assert(math.abs(result) == 10)
    }

    it("should return the IllegalArg error code") {
      val result = GDALWarp.get_data(token, dataset.value, 1, sourceWindow, destWindow, 1, 1, buffer)

      assert(math.abs(result) == 5)
    }
  }
}
