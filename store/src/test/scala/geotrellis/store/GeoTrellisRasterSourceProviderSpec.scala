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

package geotrellis.store

import geotrellis.raster.RasterSource

import org.scalatest.funspec.AnyFunSpec

class GeoTrellisRasterSourceProviderSpec extends AnyFunSpec {
  describe("GeoTrellisRasterSourceProvider") {
    val provider = new GeoTrellisRasterSourceProvider()

    it("should process a non-prefixed string") {
      assert(provider.canProcess("hdfs://storage/big-catalog?layer=big&zoom=30"))
    }

    it("should process a prefixed string") {
      assert(provider.canProcess("gt+s3://catalog/path/blah?layer=blah!&zoom=0&band_count=4"))
    }

    it("should not be able to process a path that doesn't contain a layer name") {
      assert(!provider.canProcess("file://this/path/leads/to/a/bad/catalog"))
    }

    it("should not be able to process a path that doesn't point to a catalog") {
      assert(!provider.canProcess("s3://path/to/my/fav/files/cool-image-3.jp2"))
    }

    it("should not be able to process a GDAL prefixed path") {
      assert(!provider.canProcess("gdal+file:///sketch-pad/files/temp-file.tif"))
    }

    it("should produce a GeoTrellisRasterSource from a string") {
      val params = s"?layer=landsat&zoom=0"
      val uriMultiband = s"file:///tmp/catalog$params"
      assert(RasterSource(uriMultiband).isInstanceOf[GeoTrellisRasterSource])
    }
  }
}
