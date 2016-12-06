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

package geotrellis.raster.io.geotiff

import geotrellis.util._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.testkit._

import org.scalatest._

trait Tester {
  def paths: List[String]

  class Tester(path: String) {
    val tiffTags = TiffTagsReader.read(path)
    val byteBuffer= Filesystem.toMappedByteBuffer(path)
    val arraySegmentBytes: ArraySegmentBytes =
      ArraySegmentBytes(byteBuffer, tiffTags)

    val geoTiff =
      if (tiffTags.bandCount == 1)
        SinglebandGeoTiff(path)
      else
        MultibandGeoTiff(path)

    val actual = geoTiff.imageData.segmentBytes
  }
}

class SegmentBytesSpec extends FunSpec
  with GeoTiffTestUtils
  with Matchers
  with BeforeAndAfterAll
  with RasterMatchers
  with TileBuilders
  with Tester {

  val paths = List(
    geoTiffPath("uncompressed/striped/byte.tif"),
    geoTiffPath("uncompressed/tiled/byte.tif"),
    geoTiffPath("3bands/byte/3bands-striped-band.tif"),
    geoTiffPath("3bands/byte/3bands-tiled-band.tif")
  )

  val largeFile = geoTiffPath("large-sparse-compressed.tif")

  describe("Reading into ArraySegmentBytes") {
    it("striped, singleband GeoTiff") {
      val tester = new Tester(paths(0))
      assert(tester.arraySegmentBytes.size == tester.actual.size)
    }
    it("tiled, singleband GeoTiff") {
      val tester = new Tester(paths(1))
      assert(tester.arraySegmentBytes.size == tester.actual.size)
    }
    it("striped, multiband GeoTiff") {
      val tester = new Tester(paths(2))
      assert(tester.arraySegmentBytes.size == tester.tiffTags.segmentCount)
    }
    it("tiled, multiband GeoTiff") {
      val tester = new Tester(paths(3))
      assert(tester.arraySegmentBytes.size == tester.tiffTags.segmentCount)
    }
  }
}
