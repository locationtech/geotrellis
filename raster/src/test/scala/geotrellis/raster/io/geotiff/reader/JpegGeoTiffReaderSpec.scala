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

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.testkit._

import spire.syntax.cfor._
import org.scalatest._

class JpegGeoTiffReaderSpec extends FunSpec
    with RasterMatchers
    with GeoTiffTestUtils {


  describe("Reading a geotiff with JPEG compression") {
    it("should read jpeg compressed GeoTiff") {
      // TO USE: Download
      //  https://oin-hotosm.s3.amazonaws.com/5b437bcc2b6a08001185f94c/0/5b437bcc2b6a08001185f94d.tif
      // and call it "jpeg-test.tif" in the  raster/data/geotiff-test-files folder
      // run gdal_translate -co compression=deflate jpeg-test.tif jpeg-test-deflate.tif to create our expected.

      val gt = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test.tif"))
      val actual = gt.tile.toArrayTile
      val gt2 = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-deflate.tif"))
      val expected = gt2.tile.toArrayTile

      GeoTiff(Raster(actual, gt.raster.extent), gt.crs).copy(
        options=GeoTiffOptions.DEFAULT//.copy(colorSpace=6)
      ).write("jpeg-test-written-t1.tif")
      assertEqual(actual, expected)
    }
  }
}
