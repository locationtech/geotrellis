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

package geotrellis.raster.gdal

import geotrellis.raster.RasterSource

import org.scalatest.funspec.AnyFunSpec

class GDALRasterSourceProviderSpec extends AnyFunSpec {
  describe("GDALRasterSourceProvider") {
    val provider = new GDALRasterSourceProvider()

    it("should process a VSI path") {
      assert(provider.canProcess("/vsicurl/https://path/to/some/file.tiff"))
    }

    it("should process a non-prefixed string") {
      assert(provider.canProcess("s3://bucket/thing/something/file.jp2"))
    }

    it("should process a prefixed string") {
      assert(provider.canProcess("zip+s3://bucket/thing/something/more-data.zip"))
    }

    it("should not be able to process a GeoTrellis catalog path") {
      assert(!provider.canProcess("gt+s3://path/to/my/fav/catalog?layer=fav&zoom=3"))
    }

    it("should not be able to process a GeoTiff prefixed path") {
      assert(!provider.canProcess("gtiff+file:///tmp/temp-file.tif"))
    }

    it("should produce a GDALRasterSource from a string") {
      assert(RasterSource("file:///tmp/dumping-ground/random/file.zip").isInstanceOf[GDALRasterSource])
    }
  }
}
