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

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.raster.io.geotiff.reader._

import spire.syntax.cfor._
import org.scalatest._

class SinglebandCropIteratorSpec extends FunSpec
  with Matchers
  with RasterMatchers
  with GeoTiffTestUtils {

  describe("Doing a crop iteration on a SinglebandGeoTiff") {
    val path = geoTiffPath("ls8_int32.tif")
    val geoTiff = {
      val tiff = SinglebandGeoTiff(path)
      tiff.copy(tile = tiff.tile.toArrayTile)
    }
    val cols = geoTiff.imageData.cols
    val rows = geoTiff.imageData.rows

    it("should return the correct col and row iteration numbers for divisble subsections") {
      val windowedCols = 32
      val windowedRows = 32
      val singlebandIterator = new SinglebandCropIterator(geoTiff, windowedCols, windowedRows)
      val actual = (singlebandIterator.colIterations, singlebandIterator.rowIterations)
      val expected = (16, 16)

      actual should be (expected)
    }

    it("should return the correct col and row iteration numbers for nondivisble subsections") {
      val windowedCols = 700
      val windowedRows = 650
      val singlebandIterator = new SinglebandCropIterator(geoTiff, windowedCols, windowedRows)
      val actual = (singlebandIterator.colIterations, singlebandIterator.rowIterations)
      val expected = (1, 1)

      actual should be (expected)
    }

    it("should return the correct windowedGeoTiffs with equal dimensions") {
      val windowedCols = 256
      val windowedRows = 256
      val singlebandIterator =
        new SinglebandCropIterator(geoTiff, windowedCols, windowedRows)

      val expected: Array[Tile] =
        Array(geoTiff.raster.tile.crop(0, 0, 256, 256),
          geoTiff.raster.tile.crop(256, 0, 512, 256),
          geoTiff.raster.tile.crop(0, 256, 256, 512),
          geoTiff.raster.tile.crop(256, 256, 512, 512))

      val actual: Array[Tile] =
        Array(singlebandIterator.next.tile,
          singlebandIterator.next.tile,
          singlebandIterator.next.tile,
          singlebandIterator.next.tile)

      cfor(0)(_ < actual.length, _ + 1) { i =>
        assertEqual(expected(i), actual(i))
      }
    }

    it("should return the whole thing if the inputted dimensions are larger than the cols and rows") {
      val windowedCols = 950
      val windowedRows = 1300
      val singlebandIterator =
        new SinglebandCropIterator(geoTiff, windowedCols, windowedRows)

      val expected = geoTiff.tile
      val actual = singlebandIterator.next.tile

      assertEqual(expected, actual)
    }

    it("should return the correct windowedGeoTiffs with different dimensions") {
      val windowedCols = 250
      val windowedRows = 450
      val singlebandIterator =
        new SinglebandCropIterator(geoTiff, windowedCols, windowedRows)

      val expected: Array[Tile] =
        Array(geoTiff.raster.tile.crop(0, 0, 250, 450),
          geoTiff.raster.tile.crop(250, 0, 500, 450),
          geoTiff.raster.tile.crop(500, 0, 512, 450),
          geoTiff.raster.tile.crop(0, 450, 250, 512),
          geoTiff.raster.tile.crop(250, 450, 500, 512),
          geoTiff.raster.tile.crop(500, 450, 512, 512))

      val actual: Array[Tile] =
        Array(singlebandIterator.next.tile,
          singlebandIterator.next.tile,
          singlebandIterator.next.tile,
          singlebandIterator.next.tile,
          singlebandIterator.next.tile,
          singlebandIterator.next.tile)

      cfor(0)(_ < actual.length, _ + 1) { i =>
        assertEqual(expected(i), actual(i))
      }
    }

    it("should say that there is another value when one actually exists") {
      val windowedCols = 256
      val windowedRows = 256
      val singlebandIterator =
        new SinglebandCropIterator(geoTiff, windowedCols, windowedRows)

      cfor(0)(_ < 3, _ + 1) { i =>
        singlebandIterator.next.tile
        singlebandIterator.hasNext should be (true)
      }
      singlebandIterator.next.tile
      singlebandIterator.hasNext should be (false)
    }
  }
}
