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

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.testkit._

import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec

class GDALConvertedRasterSourceSpec extends AnyFunSpec with RasterMatchers with GivenWhenThen {

  val url = Resource.path("vlm/aspect-tiled.tif")
  val uri = s"file://$url"

  val url2 = Resource.path("vlm/byte-tile.tiff")
  val uri2 = s"file://$url2"

  lazy val source: GDALRasterSource = GDALRasterSource(url)
  lazy val byteSource: GDALRasterSource = GDALRasterSource(url2)

  lazy val expectedRaster: Raster[MultibandTile] =
    GeoTiffReader
      .readMultiband(url, streaming = false)
      .raster

  lazy val expectedRaster2: Raster[MultibandTile] =
    GeoTiffReader
      .readMultiband(url2, streaming = false)
      .raster

  lazy val targetExtent = expectedRaster.extent
  lazy val targetExtent2 = expectedRaster2.extent

  /** Note:
   *  Many of these tests have a threshold of 1.0. The reason for this large value
   *  is due to the difference in how GeoTrellis and GDAL convert floating point
   *  to non-floating point values. In GeoTrellis, all floaint point values are
   *  rounded down. Thus, 17.1 -> 17.0, 256.981 -> 256, etc. Whereas GDAL always
   *  rounds the values up. Therefore, 17.1 -> 18, 256.981 -> 257, etc. This means
   *  that we need to account for this when comparing certain conversion results.
   */

  describe("Converting to a different CellType") {

    describe("Byte CellType") {
      it("should convert to: ByteConstantNoDataCellType") {
        val actual = byteSource.convert(ByteConstantNoDataCellType).read(targetExtent2).get
        val expected = byteSource.read(targetExtent2).get.mapTile { _.convert(ByteConstantNoDataCellType) }

        assertRastersEqual(actual, expected, 1.0)
      }

      it("should convert to: ByteUserDefinedNoDataCellType(-10)") {
        val actual = byteSource.convert(ByteUserDefinedNoDataCellType(-10)).read(targetExtent2).get
        val expected = byteSource.read(targetExtent2).get.mapTile { _.convert(ByteUserDefinedNoDataCellType(-10)) }

        assertRastersEqual(actual, expected, 1.0)
      }

      it("should convert to: ByteCellType") {
        val actual = byteSource.convert(ByteCellType).read(targetExtent2).get
        val expected = byteSource.read(targetExtent2).get.mapTile { _.convert(ByteCellType) }

        assertRastersEqual(actual, expected, 1.0)
      }
    }

    describe("UByte CellType") {
      it("should convert to: UByteConstantNoDataCellType") {

        val actual = byteSource.convert(UByteConstantNoDataCellType).read(targetExtent2).get
        val expected = byteSource.read(targetExtent2).get.mapTile { _.convert(UByteConstantNoDataCellType) }

        assertRastersEqual(actual, expected, 1.0)
      }

      it("should convert to: UByteUserDefinedNoDataCellType(10)") {
        val actual = byteSource.convert(UByteUserDefinedNoDataCellType(10)).read(targetExtent2).get
        val expected = byteSource.read(targetExtent2).get.mapTile { _.convert(UByteUserDefinedNoDataCellType(10)) }

        assertRastersEqual(actual, expected, 1.0)
      }

      it("should convert to: UByteCellType") {
        val actual = byteSource.convert(UByteCellType).read(targetExtent2).get
        val expected = byteSource.read(targetExtent2).get.mapTile { _.convert(UByteCellType) }

        assertRastersEqual(actual, expected, 1.0)
      }
    }

    describe("Short CellType") {
      it("should convert to: ShortConstantNoDataCellType") {
        val actual = source.convert(ShortConstantNoDataCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(ShortConstantNoDataCellType) }

        assertRastersEqual(actual, expected, 1.0)
      }

      it("should convert to: ShortUserDefinedNoDataCellType(-1)") {
        val actual = source.convert(ShortUserDefinedNoDataCellType(-1)).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(ShortUserDefinedNoDataCellType(-1)) }

        assertRastersEqual(actual, expected, 1.0)
      }

      it("should convert to: ShortCellType") {
        val actual = source.convert(ShortCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(ShortCellType) }

        assertRastersEqual(actual, expected, 1.0)
      }
    }

    describe("UShort CellType") {
      it("should convert to: UShortConstantNoDataCellType") {
        val actual = byteSource.convert(UShortConstantNoDataCellType).read(targetExtent2).get
        val expected = byteSource.read(targetExtent2).get.mapTile { _.convert(UShortConstantNoDataCellType) }

        assertRastersEqual(actual, expected, 1.0)
      }

      it("should convert to: UShortUserDefinedNoDataCellType(-1)") {
        val actual = byteSource.convert(UShortUserDefinedNoDataCellType(-1)).read(targetExtent2).get
        val expected = byteSource.read(targetExtent2).get.mapTile { _.convert(UShortUserDefinedNoDataCellType(-1)) }

        assertRastersEqual(actual, expected, 1.0)
      }

      it("should convert to: UShortCellType") {
        val actual = byteSource.convert(UShortCellType).read(targetExtent2).get
        val expected = byteSource.read(targetExtent2).get.mapTile { _.convert(UShortCellType) }

        assertRastersEqual(actual, expected)
      }
    }

    describe("Int CellType") {
      it("should convert to: IntConstantNoDataCellType") {
        val actual = source.convert(IntConstantNoDataCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(IntConstantNoDataCellType) }

        assertRastersEqual(actual, expected, 1)
      }

      it("should convert to: IntUserDefinedNoDataCellType(-100)") {
        val actual = source.convert(IntUserDefinedNoDataCellType(-100)).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(IntUserDefinedNoDataCellType(-100)) }

        assertRastersEqual(actual, expected, 1)
      }

      it("should convert to: IntCellType") {
        val actual = source.convert(IntCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(IntCellType) }

        assertRastersEqual(actual, expected, 1)
      }
    }

    describe("Float CellType") {
      it("should convert to: FloatConstantNoDataCellType") {
        val actual = source.convert(FloatConstantNoDataCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(FloatConstantNoDataCellType) }

        assertRastersEqual(actual, expected)
      }

      it("should convert to: FloatUserDefinedNoDataCellType(0)") {
        val actual = source.convert(FloatUserDefinedNoDataCellType(0)).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(FloatUserDefinedNoDataCellType(0)) }

        assertRastersEqual(actual, expected)
      }

      it("should convert to: FloatCellType") {
        val actual = source.convert(FloatCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(FloatCellType) }

        assertRastersEqual(actual, expected)
      }
    }

    describe("Double CellType") {
      it("should convert to: DoubleConstantNoDataCellType") {
        val actual = source.convert(DoubleConstantNoDataCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(DoubleConstantNoDataCellType) }

        assertRastersEqual(actual, expected)
      }

      it("should convert to: DoubleUserDefinedNoDataCellType(1.0)") {
        val actual = source.convert(DoubleUserDefinedNoDataCellType(1.0)).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(DoubleUserDefinedNoDataCellType(1.0)) }

        assertRastersEqual(actual, expected)
      }

      it("should convert to: DoubleCellType") {
        val actual = source.convert(DoubleCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(DoubleCellType) }

        assertRastersEqual(actual, expected)
      }
    }
  }
}

