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

package geotrellis.raster.io

import geotrellis.util.Filesystem

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class FilesystemSpec extends AnyFunSpec with Matchers {
  describe("Filesystem") {
    it("should give the same array for slurp and mapToByteArray for whole array") {
      val path = "raster/data/fake.img32.json"
      val bytes1 = Filesystem.slurp(path)
      val bytes2 = Array.ofDim[Byte](bytes1.size)
      Filesystem.mapToByteArray(path,bytes2,0,bytes2.size)
      bytes1 should be (bytes2)
    }
  }
}
