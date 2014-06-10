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

package geotrellis.data

import geotrellis._
import geotrellis.feature.Extent
import geotrellis.raster._

import org.scalatest._

class AsciiSpec extends FunSpec 
                   with Matchers {
  val data:Array[Int] = (1 to 100).toArray

  val e = Extent(19.0, 9.0, 49.0, 39.0)
  val g = RasterExtent(e, 3.0, 3.0, 10, 10)
  val r = Raster(data, g)

  describe("An AsciiReader") {
    it ("should fail on non-existent files") {
      val path = "/does/not/exist.tif"
      an [Exception] should be thrownBy {
        AsciiRasterLayerBuilder.fromFile(path) 
      }
    }

    it ("should write ASCII") {
      AsciiWriter.write("/tmp/foo.asc", r, "foo")
    }

    it ("should read ASCII") {
      val r2 = AsciiRasterLayerBuilder.fromFile("/tmp/foo.asc").getRaster()
      r2 should be (r)
    }

  }
}
