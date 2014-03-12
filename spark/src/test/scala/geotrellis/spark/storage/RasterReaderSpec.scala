/**************************************************************************
 * Copyright (c) 2014 Digital Globe.
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
 **************************************************************************/

package geotrellis.spark.storage
import geotrellis.spark.TestEnvironment
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.testfiles.AllOnes

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class RasterReaderSpec extends FunSpec with TestEnvironment with ShouldMatchers {

  private def read(start: Long, end: Long): Int = {
    val allOnes = AllOnes(inputHome, conf).path
    val reader = RasterReader(allOnes, conf, TileIdWritable(start), TileIdWritable(end))
    val numEntries = reader.count(_ => true)
    reader.close()
    numEntries
  }

  describe("RasterReader") {
    it("should retrieve all entries") {
      read(208787, 211861) should be(12)
    }
    it("should retrieve all entries when no range is specified") {
      read(Long.MinValue, Long.MaxValue) should be(12)
    }
    it("should handle a non-existent start and end") {
      read(0, 209810) should be(3)
    }
    it("should be able to skip a partition") {
      read(210838, Long.MaxValue) should be(3)
    }
    it("should be handle start=end") {
      read(209811, 209811) should be(1)
    }
  }
}