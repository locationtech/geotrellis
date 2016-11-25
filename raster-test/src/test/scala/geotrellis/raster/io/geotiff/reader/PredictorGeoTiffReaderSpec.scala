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

class PredictorGeoTiffReaderSpec extends FunSpec 
    with RasterMatchers
    with GeoTiffTestUtils {


  describe("Reading a geotiff with compression and predictor=2") {
    def read(compressionType: String, bandType: String): (Tile, Tile) = {
      val actual = GeoTiffReader.readSingleband(geoTiffPath(s"predictor/predictor2/$compressionType-${bandType}.tif")).tile;
      val expected = GeoTiffReader.readSingleband(geoTiffPath(s"predictor/uncompressed-${bandType}.tif")).tile;
      (actual, expected)
    }

    it("should read lzw byte with predictor=2 and match uncompressed") {
      val (actual, expected) = read("lzw", "byte")
      assertEqual(actual, expected);
    }

    it("should read deflate byte with predictor=2 and match uncompressed") {
      val (actual, expected) = read("deflate", "byte")
      assertEqual(actual, expected);
    }

    it("should read deflate byte tiled with predictor=2 and match uncompressed") {
      val expected = GeoTiffReader.readSingleband(geoTiffPath(s"predictor/predictor2/deflate-byte-tiled.tif")).tile;
      val actual = GeoTiffReader.readSingleband(geoTiffPath(s"predictor/uncompressed-byte.tif")).tile;
      assertEqual(actual, expected);
    }

    it("should read lzw uint16 with predictor=2 and match uncompressed") {
      val (actual, expected) = read("lzw", "uint16")
      assertEqual(actual, expected);
    }

    it("should read deflate uint16 with predictor=2 and match uncompressed") {
      val (actual, expected) = read("deflate", "uint16")
      assertEqual(actual, expected);
    }

    it("should read lzw int16 with predictor=2 and match uncompressed") {
      val (actual, expected) = read("lzw", "int16")
      assertEqual(actual, expected);
    }

    it("should read deflate int16 with predictor=2 and match uncompressed") {
      val (actual, expected) = read("deflate", "int16")
      assertEqual(actual, expected);
    }

    it("should read lzw uint32 with predictor=2 and match uncompressed") {
      val (actual, expected) = read("lzw", "uint32")
      assertEqual(actual, expected);
    }

    it("should read deflate uint32 with predictor=2 and match uncompressed") {
      val (actual, expected) = read("deflate", "uint32")
      assertEqual(actual, expected);
    }

    it("should read lzw int32 with predictor=2 and match uncompressed") {
      val (actual, expected) = read("lzw", "int32")
      assertEqual(actual, expected);
    }

    it("should read deflate int32 with predictor=2 and match uncompressed") {
      val (actual, expected) = read("deflate", "int32")
      assertEqual(actual, expected);
    }

    it("should read lzw float32 with predictor=2 and match uncompressed") {
      val (actual, expected) = read("lzw", "float32")
      assertEqual(actual, expected);
    }

    it("should read deflate float32 with predictor=2 and match uncompressed") {
      val (actual, expected) = read("deflate", "float32")
      assertEqual(actual, expected);
    }

    it("should read lzw float32 with negative values with predictor=2 and match uncompressed") {
      val expected = GeoTiffReader.readSingleband(geoTiffPath(s"predictor/predictor2/lzw-float32-neg.tif")).tile;
      val actual = GeoTiffReader.readSingleband(geoTiffPath(s"predictor/uncompressed-float32-neg.tif")).tile;
      assertEqual(actual, expected);
    }
  }

  describe("Reading a geotiff with compression and predictor=3") {
    def read(compressionType: String, bandType: String): (Tile, Tile) = {
      val actual = GeoTiffReader.readSingleband(geoTiffPath(s"predictor/predictor3/$compressionType-${bandType}.tif")).tile;
      val expected = GeoTiffReader.readSingleband(geoTiffPath(s"predictor/uncompressed-${bandType}.tif")).tile;
      (actual, expected)
    }

    it("should read lzw float32 with predictor=3 and match uncompressed") {
      val (actual, expected) = read("lzw", "float32")
      assertEqual(actual, expected);
    }

    it("should read deflate float32 with predictor=3 and match uncompressed") {
      val (actual, expected) = read("deflate", "float32")
      assertEqual(actual, expected);
    }

    it("should read lzw float64 with predictor=3 and match uncompressed") {
      val (actual, expected) = read("lzw", "float64")
      assertEqual(actual, expected);
    }

    it("should read deflate float64 with predictor=3 and match uncompressed") {
      val (actual, expected) = read("deflate", "float64")
      assertEqual(actual, expected);
    }
  }
}
