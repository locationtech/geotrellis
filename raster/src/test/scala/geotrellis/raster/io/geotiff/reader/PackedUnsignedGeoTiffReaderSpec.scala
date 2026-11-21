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

import geotrellis.raster.io.geotiff.*
import geotrellis.raster.io.geotiff.compression.*
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.raster.io.geotiff.tags.codes.SampleFormat.{SignedInt, UnsignedInt}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.Paths
import java.nio.ByteOrder

class PackedUnsignedGeoTiffReaderSpec extends AnyFunSpec with Matchers {
  // GDAL-generated 19x18 big-endian TIFFs with 16x16 tiles.
  private val cases = Seq(
    (2, "none", "pixel", 1), (7, "packbits", "pixel", 1),
    (9, "lzw", "pixel", 1), (12, "zstd", "pixel", 1),
    (14, "deflate", "pixel", 1), (17, "lzw", "pixel", 1),
    (24, "deflate", "pixel", 1), (31, "zstd", "pixel", 1),
    (5, "deflate", "pixel", 3), (11, "lzw", "band", 3)
  )

  private def path(bits: Int, compression: String, interleave: String): String =
    Paths.get(getClass.getResource(s"/packed-u$bits-$compression-$interleave.tif").toURI).toString

  private def value(bits: Int, band: Int, col: Int, row: Int): Double = {
    val max = (1L << bits) - 1
    val raw = if (col == 0 && row == 0) max else (col * 7919L + row * 104729L + band * 65537L) & max
    if (bits > 16) raw.toFloat.toDouble else raw.toDouble
  }

  private def check(tiff: MultibandGeoTiff, bits: Int, bands: Int): Unit = {
    tiff.tile.bandCount shouldBe bands
    for (band <- 0 until bands; row <- 0 until 18; col <- 0 until 19)
      withClue(s"$bits bits, band $band, pixel ($col, $row): ") {
        tiff.tile.band(band).getDouble(col, row) shouldBe value(bits, band, col, row)
      }
  }

  describe("packed unsigned samples") {
    it("maps supported widths to their storage band types") {
      for (bits <- 2 to 7) BandType(bits, UnsignedInt) shouldBe UByteBandType
      for (bits <- 9 to 15) BandType(bits, UnsignedInt) shouldBe UInt16BandType
      for (bits <- 17 to 31) BandType(bits, UnsignedInt) shouldBe UInt32BandType
      for (bits <- Seq(2, 9, 17, 31))
        intercept[UnsupportedOperationException] { BandType(bits, SignedInt) }
    }

    it("reads supported compressions and interleaves, including partial edge tiles") {
      for ((bits, compression, interleave, bands) <- cases; streaming <- Seq(false, true)) {
        val file = path(bits, compression, interleave)
        val source = GeoTiffReader.readMultiband(file, streaming)
        check(source, bits, bands)
        val firstBand = GeoTiffReader.readSingleband(file, streaming).tile
        for (row <- 0 until 18; col <- 0 until 19)
          withClue(s"$bits bits, first band, pixel ($col, $row): ") {
            firstBand.getDouble(col, row) shouldBe value(bits, 0, col, row)
          }
        val written = source.toByteArray
        TiffTags.read(written).bitsPerSample shouldBe (if (bits < 8) 8 else if (bits < 16) 16 else 32)
        check(GeoTiffReader.readMultiband(written), bits, bands)
      }
    }

    it("reads GDAL's 24-bit samples in little-endian TIFFs") {
      val file = Paths.get(getClass.getResource("/packed-u24-deflate-little.tif").toURI).toString
      check(GeoTiffReader.readMultiband(file), 24, 1)
    }

    it("unpacks every supported width without losing high bits") {
      for (bits <- (2 to 7) ++ (9 to 15) ++ (17 to 31)) {
        val mask = (1L << bits) - 1
        val values = (0 until 16).map(i => if (i == 0) mask else (i * 104729L) & mask)
        val packed = new Array[Byte](bits * 2)
        for (i <- values.indices; bit <- 0 until bits) {
          val pos = i * bits + bit
          if (((values(i) >>> (bits - bit - 1)) & 1) != 0)
            packed(pos / 8) = (packed(pos / 8) | (1 << (7 - pos % 8))).toByte
        }
        val bytesPerSample = if (bits < 8) 1 else if (bits < 16) 2 else 4
        val unpacker = new PackedUnsignedDecompressor(
          NoCompressor, Array(packed.length), bits, bytesPerSample, ByteOrder.LITTLE_ENDIAN
        )
        val unpacked = unpacker.decompress(packed, 0)
        for (i <- values.indices) {
          val actual = (0 until bytesPerSample).foldLeft(0L) { (v, j) =>
            (v << 8) | (unpacked(i * bytesPerSample + j) & 0xffL)
          }
          withClue(s"$bits-bit sample $i: ") { actual shouldBe values(i) }
        }
      }
    }

    it("rejects striped packed samples rather than ignoring row padding") {
      val file = Paths.get(getClass.getResource("/packed-u3-striped.tif").toURI).toString
      val error = intercept[GeoTiffReaderLimitationException] { GeoTiffReader.readSingleband(file) }
      error.getMessage should include ("3-bit")
    }

    it("rejects unsupported fill order, predictor, and compression") {
      val tags = TiffTags.read(path(9, "lzw", "pixel"))
      val invalid = Seq(
        tags.copy(nonBasicTags = tags.nonBasicTags.copy(fillOrder = 2)),
        tags.copy(nonBasicTags = tags.nonBasicTags.copy(predictor = Some(2))),
        tags.copy(basicTags = tags.basicTags.copy(compression = 7))
      )
      for (tiff <- invalid)
        intercept[GeoTiffReaderLimitationException] { Decompressor(tiff, ByteOrder.BIG_ENDIAN) }
    }

    it("rejects truncated packed data for each decompressor") {
      val sizes = Array(8)
      val decoders = Seq[Decompressor](
        NoCompressor,
        new DeflateDecompressor(sizes, strict = true),
        new LZWDecompressor(sizes, strict = true),
        new PackBitsDecompressor(sizes, strict = true),
        ZStdCompression.createDecompressor
      )
      for (decoder <- decoders) {
        val unpacker = new PackedUnsignedDecompressor(decoder, sizes, 2, 1, ByteOrder.BIG_ENDIAN)
        intercept[MalformedGeoTiffException] { unpacker.decompress(Array.emptyByteArray, 0) }
      }
    }

    it("rejects partially decompressed packed segments") {
      for ((bits, compression) <- Seq((2, "none"), (7, "packbits"), (9, "lzw"), (14, "deflate"))) {
        val tile = GeoTiffReader.readSingleband(path(bits, compression, "pixel")).tile.asInstanceOf[GeoTiffTile]
        val segment = tile.segmentBytes.getSegment(0)
        val truncated = segment.take(segment.length / 2)
        intercept[MalformedGeoTiffException] { tile.decompressor.decompress(truncated, 0) }
      }
    }
  }
}
