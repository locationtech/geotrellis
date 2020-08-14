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

package geotrellis.util

import java.net.URI

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class FileRangeReaderProviderSpec extends AnyFunSpec with Matchers {
  describe("FileRangeReaderProviderSpec") {
    val path = "raster/data/aspect.tif"

    it("should create a FileRangeReader from a URI") {
      val reader = RangeReader(new URI(path))

      assert(reader.isInstanceOf[FileRangeReader])
    }

    it("should be able to process a URI with a scheme and an authority") {
      val expectedPath = "data/path/to/my/data/blah.tif"
      val reader = RangeReader(new URI(s"file://$expectedPath"))

      reader.asInstanceOf[FileRangeReader].file.toString should be (expectedPath)
    }

    it("should be able to process a URI with a scheme and no authority") {
      val expectedPath = "data/path/to/my/data/blah.tif"
      val reader = RangeReader(new URI(s"file:$expectedPath"))

      reader.asInstanceOf[FileRangeReader].file.toString should be (expectedPath)
    }

    it("should be able to process a URI that's a relative path") {
      val expectedPath = "../data/path/to/my/data/blah.tif"
      val reader = RangeReader(new URI(s"$expectedPath"))

      reader.asInstanceOf[FileRangeReader].file.toString should be (expectedPath)
    }

    it("should be able to process a URI with a scheme and authority that's an absolute path") {
      val expectedPath = "/data/path/to/my/data/blah.tif"
      val reader = RangeReader(new URI(s"file://$expectedPath"))

      reader.asInstanceOf[FileRangeReader].file.toString should be (expectedPath)
    }

    it("should be able to process a URI with a scheme and no authority that's an absolute path") {
      val expectedPath = "/data/path/to/my/data/blah.tif"
      val reader = RangeReader(new URI(s"file:$expectedPath"))

      reader.asInstanceOf[FileRangeReader].file.toString should be (expectedPath)
    }

    it("should be able to process a URI that's an absolute path") {
      val expectedPath = "/data/path/to/my/data/blah.tif"
      val reader = RangeReader(new URI(s"$expectedPath"))

      reader.asInstanceOf[FileRangeReader].file.toString should be (expectedPath)
    }
  }
}
