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

package geotrellis.io.geotiff

import geotrellis.raster._
import geotrellis.source._
import geotrellis.process._
import geotrellis.testkit._

import scala.io.{Source, Codec}

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

import scalaxy.loops._

class GeoTiffReaderSpec extends FunSpec
    with MustMatchers
    with RasterBuilders
    with TestServer {

  private def read(filePath: String) {
    val source = Source.fromFile(filePath)(Codec.ISO8859)
    val geoTiff = GeoTiffReader(source).read
    source.close()

    assert(geoTiff != null)
  }

  describe("read geotiffs") {
    /*it("reads econic.tif without errors") {
      read("core-test/data/econic.tif")
    }

    it("reads aspect.tif without errors") {
      read("core-test/data/aspect.tif")
    }

    it("reads slope.tif without errors") {
      read("core-test/data/slope.tif")
    }*/

    it("reads packbits.tif without errors") {
      read("core-test/data/packbits.tif")
    }
  }
}
