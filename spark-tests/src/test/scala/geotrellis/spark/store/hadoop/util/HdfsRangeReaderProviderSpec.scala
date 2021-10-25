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

package geotrellis.spark.store.hadoop.util

import geotrellis.store.hadoop.util.HdfsRangeReader
import geotrellis.util.RangeReader

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class HdfsRangeReaderProviderSpec extends AnyFunSpec with Matchers {
  describe("HdfsRangeReaderProviderSpec") {
    val uri = new java.net.URI("hdfs+file:/tmp/catalog")

    it("should create a HdfsRangeReader from a URI") {
      val reader = RangeReader(uri)

      assert(reader.isInstanceOf[HdfsRangeReader])
    }
  }
}
