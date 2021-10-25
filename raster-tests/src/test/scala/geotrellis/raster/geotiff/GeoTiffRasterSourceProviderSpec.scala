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

package geotrellis.raster.geotiff

import geotrellis.raster.RasterSource

import org.scalatest.funspec.AnyFunSpec

class GeoTiffRasterSourceProviderSpec extends AnyFunSpec {
  describe("GeoTiffRasterSourceProvider") {
    val provider = new GeoTiffRasterSourceProvider()

    it("should process a non-prefixed string") {
      assert(provider.canProcess("file:///tmp/path/to/random/file.tiff"))
    }

    it("should process a prefixed string") {
      assert(provider.canProcess("gtiff+s3://my-files/tiffs/big-tiff.TIFF"))
    }

    it("should process a non-prefixed relative path") {
      assert(provider.canProcess("../../my-file.tif"))
    }

    it("should process a prefixed relative path") {
      assert(provider.canProcess("gtiff+../../my-file.tif"))
    }

    it("should not be able to process a GDAL prefixed path") {
      assert(!provider.canProcess("gdal+file:///tmp/temp-file.tif"))
    }

    it("should produce a GeoTiffRasterSource from a string") {
      assert(RasterSource("file://dumping-ground/part-2/random/file.tiff").isInstanceOf[GeoTiffRasterSource])
    }
  }
}
