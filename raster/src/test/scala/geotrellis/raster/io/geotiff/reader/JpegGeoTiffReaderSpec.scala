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

package geotrellis.raster.io.geotiff.reader

import geotrellis.raster.io.geotiff._
import geotrellis.raster.testkit._

import org.scalatest.funspec.AnyFunSpec

class JpegGeoTiffReaderSpec extends AnyFunSpec with RasterMatchers with GeoTiffTestUtils {

  describe("Reading a geotiff with JPEG compression") {
    it("should read and write jpeg compressed GeoTiff") {
      // TO USE: Download
      //  https://oin-hotosm.s3.amazonaws.com/5b437bcc2b6a08001185f94c/0/5b437bcc2b6a08001185f94d.tif
      // and call it "jpeg-test.tif" in the  raster/data/geotiff-test-files folder
      // run gdal_translate -co compression=deflate jpeg-test.tif jpeg-test-deflate.tif to create our expected.

      val gt = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-small.tif"))
      val actual = gt.tile.toArrayTile
      // gdal_translate jpeg-test-small.tif jpeg-test-small-uncompressed.tif
      val gt2 = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-small-uncompressed.tif"))
      val expected = gt2.tile.toArrayTile

      // val gt2 = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-deflate-small.tif"))
      // val expected = gt2.tile.toArrayTile

      /*val jpegTestWrittenPath = s"$testDirPath/jpeg-test-written.tif"
      GeoTiff(Raster(actual, gt.raster.extent), gt.crs).copy(
        options=GeoTiffOptions.DEFAULT //.copy(colorSpace=6)
      ).write(jpegTestWrittenPath)*/

      // yes, this test looks a bit weird now
      // val gt3 = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-written.tif"))
      // val actualRead = gt3.tile.toArrayTile

      assertEqual(actual, expected)
    }
  }
}
