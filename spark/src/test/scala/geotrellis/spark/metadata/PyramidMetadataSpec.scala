/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.metadata
import geotrellis.Extent
import geotrellis.spark.TestEnvironment
import geotrellis.spark.tiling.PixelExtent
import geotrellis.spark.tiling.TileExtent

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import java.awt.image.DataBuffer

class PyramidMetadataSpec extends FunSpec with TestEnvironment with ShouldMatchers {

  describe("PyramidMetadata.save") {

    it("should correctly save and read the metadata") {
      val meta = PyramidMetadata(
        Extent(1, 1, 1, 1),
        512,
        1,
        Double.NaN,
        DataBuffer.TYPE_FLOAT,
        10,
        Map("1" -> new RasterMetadata(PixelExtent(0, 0, 0, 0), TileExtent(0, 0, 0, 0))))

      meta.save(outputLocal, conf)

      val newMeta = PyramidMetadata(outputLocal, conf)
      
      meta should be(newMeta)
    }

  }
}