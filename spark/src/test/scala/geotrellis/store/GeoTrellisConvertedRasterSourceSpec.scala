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

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.testkit._

import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec

class GeoTrellisConvertedRasterSourceSpec extends AnyFunSpec with RasterMatchers with GivenWhenThen with CatalogTestEnvironment {
  val layerId = LayerId("landsat", 0)
  val uriMultiband = s"file://${TestCatalog.multibandOutputPath}?layer=${layerId.name}&zoom=${layerId.zoom}"

  lazy val source = new GeoTrellisRasterSource(uriMultiband)

  lazy val expectedRaster: Raster[MultibandTile] =
    GeoTiffReader
      .readMultiband(TestCatalog.filePath, streaming = false)
      .raster

  describe("Converting to a different CellType") {
    lazy val targetExtent = expectedRaster.extent

    lazy val expectedTile: MultibandTile = expectedRaster.tile

    describe("Bit CellType") {
      it("should convert to: ByteCellType") {
        val actual = source.convert(BitCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(BitCellType) }

        assertEqual(actual, expected)
      }
    }

    describe("Byte CellType") {
      it("should convert to: ByteConstantNoDataCellType") {
        val actual = source.convert(ByteConstantNoDataCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(ByteConstantNoDataCellType) }

        assertEqual(actual, expected)
      }

      it("should convert to: ByteUserDefinedNoDataCellType(10)") {
        val actual = source.convert(ByteUserDefinedNoDataCellType(10)).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(ByteUserDefinedNoDataCellType(10)) }

        assertEqual(actual, expected)
      }

      it("should convert to: ByteCellType") {
        val actual = source.convert(ByteCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(ByteCellType) }

        assertEqual(actual, expected)
      }
    }

    describe("UByte CellType") {
      it("should convert to: UByteConstantNoDataCellType") {
        val actual = source.convert(UByteConstantNoDataCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(UByteConstantNoDataCellType) }

        assertEqual(actual, expected)
      }

      it("should convert to: UByteUserDefinedNoDataCellType(10)") {
        val actual = source.convert(UByteUserDefinedNoDataCellType(10)).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(UByteUserDefinedNoDataCellType(10)) }

        assertEqual(actual, expected)
      }

      it("should convert to: UByteCellType") {
        val actual = source.convert(UByteCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(UByteCellType) }

        assertEqual(actual, expected)
      }
    }

    describe("Short CellType") {
      it("should convert to: ShortConstantNoDataCellType") {
        val actual = source.convert(ShortConstantNoDataCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(ShortConstantNoDataCellType) }

        assertEqual(actual, expected)
      }

      it("should convert to: ShortUserDefinedNoDataCellType(-1)") {
        val actual = source.convert(ShortUserDefinedNoDataCellType(-1)).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(ShortUserDefinedNoDataCellType(-1)) }

        assertEqual(actual, expected)
      }

      it("should convert to: ShortCellType") {
        val actual = source.convert(ShortCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(ShortCellType) }

        assertEqual(actual, expected)
      }
    }

    describe("UShort CellType") {
      it("should convert to: UShortConstantNoDataCellType") {
        val actual = source.convert(UShortConstantNoDataCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(UShortConstantNoDataCellType) }

        assertEqual(actual, expected)
      }

      it("should convert to: UShortUserDefinedNoDataCellType(-1)") {
        val actual = source.convert(UShortUserDefinedNoDataCellType(-1)).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(UShortUserDefinedNoDataCellType(-1)) }

        assertEqual(actual, expected)
      }

      it("should convert to: UShortCellType") {
        val actual = source.convert(UShortCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(UShortCellType) }

        assertEqual(actual, expected)
      }
    }

    describe("Int CellType") {
      it("should convert to: IntConstantNoDataCellType") {
        val actual = source.convert(IntConstantNoDataCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(IntConstantNoDataCellType) }

        assertEqual(actual, expected)
      }

      it("should convert to: IntUserDefinedNoDataCellType(-100)") {
        val actual = source.convert(IntUserDefinedNoDataCellType(-100)).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(IntUserDefinedNoDataCellType(-100)) }

        assertEqual(actual, expected)
      }

      it("should convert to: IntCellType") {
        val actual = source.convert(IntCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(IntCellType) }

        assertEqual(actual, expected)
      }
    }

    describe("Float CellType") {
      it("should convert to: FloatConstantNoDataCellType") {
        val actual = source.convert(FloatConstantNoDataCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(FloatConstantNoDataCellType) }

        assertEqual(actual, expected)
      }

      it("should convert to: FloatUserDefinedNoDataCellType(0)") {
        val actual = source.convert(FloatUserDefinedNoDataCellType(0)).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(FloatUserDefinedNoDataCellType(0)) }

        assertEqual(actual, expected)
      }

      it("should convert to: FloatCellType") {
        val actual = source.convert(FloatCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(FloatCellType) }

        assertEqual(actual, expected)
      }
    }

    describe("Double CellType") {
      it("should convert to: DoubleConstantNoDataCellType") {
        val actual = source.convert(DoubleConstantNoDataCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(DoubleConstantNoDataCellType) }

        assertEqual(actual, expected)
      }

      it("should convert to: DoubleUserDefinedNoDataCellType(1.0)") {
        val actual = source.convert(DoubleUserDefinedNoDataCellType(1.0)).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(DoubleUserDefinedNoDataCellType(1.0)) }

        assertEqual(actual, expected)
      }

      it("should convert to: DoubleCellType") {
        val actual = source.convert(DoubleCellType).read(targetExtent).get
        val expected = source.read(targetExtent).get.mapTile { _.convert(DoubleCellType) }

        assertEqual(actual, expected)
      }
    }
  }

  describe("Chaining together operations") {
    lazy val targetCellType = DoubleConstantNoDataCellType
    lazy val targetExtent = expectedRaster.extent.reproject(source.crs, WebMercator)
    lazy val expectedTile: MultibandTile = expectedRaster.tile

    it("should have the correct CellType after reproject") {
      val actual = source.convert(targetCellType).reproject(WebMercator).read(targetExtent).get.cellType

      actual should be (targetCellType)
    }

    it("should have the correct CellType after multiple conversions") {
      val actual =
        source
          .convert(FloatUserDefinedNoDataCellType(0))
          .reproject(WebMercator)
          .convert(targetCellType)
          .read(targetExtent).get.cellType

      actual should be (targetCellType)
    }
  }
}
