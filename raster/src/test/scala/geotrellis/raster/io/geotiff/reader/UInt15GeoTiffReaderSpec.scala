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
import geotrellis.raster.geotiff.GeoTiffRasterSource
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags.codes.SampleFormat.{SignedInt, UnsignedInt}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import java.nio.{ByteBuffer, ByteOrder}
import java.nio.file.{Files, Paths}
import java.util.zip.Inflater

class UInt15GeoTiffReaderSpec extends AnyFunSpec with Matchers {
  // Both fixtures contain the same synthetic values and GDAL-generated overviews;
  // uint15-tiled.tif uses NBITS=15 while uint15-expected.tif uses standard UInt16.
  private def fixture(name: String): String =
    Paths.get(getClass.getResource(s"/$name").toURI).toString

  private val packedPath = fixture("uint15-tiled.tif")
  private val expectedPath = fixture("uint15-expected.tif")

  private def assertTile(actual: Tile, expected: Tile): Unit = {
    actual.cols shouldBe expected.cols
    actual.rows shouldBe expected.rows
    actual.cellType shouldBe expected.cellType
    for (row <- 0 until actual.rows; col <- 0 until actual.cols)
      withClue(s"pixel ($col, $row): ") { actual.get(col, row) shouldBe expected.get(col, row) }
  }

  private def assertCompleteDeflate(tile: GeoTiffTile): Unit = {
    tile.segmentBytes.foreach { segment =>
      val inflater = new Inflater()
      try {
        inflater.setInput(segment)
        val buffer = new Array[Byte](1024)
        while (!inflater.finished()) {
          val count = inflater.inflate(buffer)
          withClue("Incomplete DEFLATE stream: ") { (count > 0 || inflater.finished()) shouldBe true }
        }
      } finally {
        inflater.end()
      }
    }
  }

  describe("15-bit unsigned GeoTIFFs") {
    it("maps 15-bit unsigned samples to the UInt16 band type") {
      BandType(15, UnsignedInt) shouldBe UInt16BandType
      intercept[UnsupportedOperationException] { BandType(15, SignedInt) }
    }

    it("reads tiled samples, NoData, and overviews in both singleband modes") {
      val header = ByteBuffer.wrap(Files.readAllBytes(Paths.get(packedPath))).order(ByteOrder.LITTLE_ENDIAN)
      val ifd = header.getInt(4)
      val count = header.getShort(ifd) & 0xffff
      val bitsEntry = (0 until count).map(i => ifd + 2 + i * 12)
        .find(i => (header.getShort(i) & 0xffff) == 258).get
      (header.getShort(bitsEntry + 8) & 0xffff) shouldBe 15
      val expected = GeoTiffReader.readSingleband(expectedPath)
      for (streaming <- Seq(false, true)) {
        val actual = GeoTiffReader.readSingleband(packedPath, streaming)
        actual.cellType shouldBe UShortConstantNoDataCellType
        actual.tile.get(0, 0) shouldBe NODATA
        actual.tile.get(1, 0) shouldBe 32767
        assertTile(actual.tile, expected.tile)
        actual.overviews.length shouldBe 4
        actual.overviews.zip(expected.overviews).foreach { case (a, e) => assertTile(a.tile, e.tile) }
        val tile = actual.tile.asInstanceOf[GeoTiffTile]
        tile.overviews.length shouldBe 4
        tile.overviews.zip(expected.overviews).foreach { case (a, e) => assertTile(a, e.tile) }
        assertTile(actual.crop(14, 14, 19, 19).tile, expected.crop(14, 14, 19, 19).tile)
      }
    }

    it("reads the same samples through the multiband reader") {
      val expected = GeoTiffReader.readMultiband(expectedPath)
      for (streaming <- Seq(false, true)) {
        val actual = GeoTiffReader.readMultiband(packedPath, streaming)
        actual.tile.bandCount shouldBe 1
        assertTile(actual.tile.band(0), expected.tile.band(0))
        actual.overviews.length shouldBe 4
        actual.overviews.zip(expected.overviews).foreach { case (a, e) =>
          assertTile(a.tile.band(0), e.tile.band(0))
        }
        val tile = actual.tile.asInstanceOf[GeoTiffMultibandTile]
        tile.overviews.length shouldBe 4
        tile.overviews.zip(expected.overviews).foreach { case (a, e) =>
          assertTile(a.band(0), e.tile.band(0))
        }
      }
    }

    it("reads a streamed raster-source window crossing tile boundaries") {
      val bounds = GridBounds(14L, 14L, 19L, 19L)
      val actual = GeoTiffRasterSource(packedPath).read(bounds).get.tile.band(0)
      val expected = GeoTiffRasterSource(expectedPath).read(bounds).get.tile.band(0)
      assertTile(actual, expected)
    }

    it("writes valid 16-bit samples and overviews in both TIFF layouts") {
      val expected = GeoTiffReader.readSingleband(expectedPath)
      val sources = Seq(
        GeoTiffReader.readSingleband(packedPath, true),
        GeoTiffReader.readMultiband(packedPath, true)
      )
      for (source <- sources; optimized <- Seq(false, true)) {
        val bytes = geotrellis.raster.io.geotiff.writer.GeoTiffWriter.write(source, optimized)
        val header = ByteBuffer.wrap(bytes)
        val ifd = header.getInt(4)
        val count = header.getShort(ifd) & 0xffff
        val bitsEntry = (0 until count).map(i => ifd + 2 + i * 12)
          .find(i => (header.getShort(i) & 0xffff) == 258).get
        (header.getShort(bitsEntry + 8) & 0xffff) shouldBe 16
        val written = GeoTiffReader.readSingleband(bytes)
        assertTile(written.tile, expected.tile)
        written.overviews.length shouldBe 4
        written.overviews.zip(expected.overviews).foreach { case (a, e) => assertTile(a.tile, e.tile) }
        assertCompleteDeflate(written.tile.asInstanceOf[GeoTiffTile])
        written.overviews.foreach(o => assertCompleteDeflate(o.tile.asInstanceOf[GeoTiffTile]))
      }
    }

    it("rejects unsupported predictors for packed 15-bit samples") {
      val bytes = Files.readAllBytes(Paths.get(packedPath))
      val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
      val ifd = header.getInt(4)
      val count = header.getShort(ifd) & 0xffff
      val predictor = (0 until count).map(i => ifd + 2 + i * 12)
        .find(i => (header.getShort(i) & 0xffff) == 317).get
      header.putShort(predictor + 8, 2.toShort)
      val error = intercept[GeoTiffReaderLimitationException] {
        GeoTiffReader.readSingleband(bytes)
      }
      error.getMessage should include ("15-bit")
    }

    it("rejects writing mixed-endian normalized overviews") {
      val packed = GeoTiffReader.readSingleband(packedPath)
      val standard = GeoTiffReader.readSingleband(expectedPath)
      for (tiff <- Seq(
        packed.copy(overviews = standard.overviews.take(1)),
        standard.copy(overviews = packed.overviews.take(1))
      )) {
        val error = intercept[IllegalArgumentException] { tiff.toByteArray }
        error.getMessage should include ("mixed output byte orders")
      }
    }

    it("rejects a truncated compressed segment") {
      val unpacker = new geotrellis.raster.io.geotiff.compression.UInt15Decompressor(Array(480))
      intercept[MalformedGeoTiffException] {
        unpacker.decompress(Array[Byte](0x78, 0x9c.toByte), 0)
      }
    }
  }
}
