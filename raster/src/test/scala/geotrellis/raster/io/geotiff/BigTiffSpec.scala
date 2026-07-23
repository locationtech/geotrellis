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

import geotrellis.proj4.{CRS, LatLng}
import geotrellis.raster.io.geotiff.compression.{Decompressor, NoCompressor}
import geotrellis.raster.{CellType, IntConstantNoDataCellType, TileLayout}
import geotrellis.util._
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.testkit._
import geotrellis.vector.Extent
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.prop.TableDrivenPropertyChecks

import java.io.File

class BigTiffSpec extends AnyFunSpec with RasterMatchers with BeforeAndAfterAll with GeoTiffTestUtils with TableDrivenPropertyChecks {
  override def afterAll(): Unit = purge

  describe("Reading BigTiffs") {
    val smallPath = geoTiffPath("ls8_int32.tif")
    val bigPath = geoTiffPath("bigtiffs/ls8_int32-big.tif")

    val smallPathMulti = geoTiffPath("multi.tif")
    val bigPathMulti = geoTiffPath("bigtiffs/multi-big.tif")

    val chunkSize = 500

    it("should read in the entire SinglebandGeoTiff") {
      val local = FileRangeReader(bigPath)
      val reader = StreamingByteReader(local, chunkSize)
      val actual = SinglebandGeoTiff(reader)
      val expected = SinglebandGeoTiff(smallPath)

      assertEqual(actual.tile.toArrayTile(), expected.tile.toArrayTile())
    }

    it("should read in a cropped SinlebandGeoTiff from the edge") {
      val local = FileRangeReader(bigPath)
      val reader = StreamingByteReader(local, chunkSize)
      val tiffTags = TiffTags.read(smallPath)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin, extent.ymin, extent.xmin + 100, extent.ymin + 100)

      val actual = SinglebandGeoTiff(reader, e)
      val expected = SinglebandGeoTiff(smallPath, e)

      assertEqual(actual.tile.toArrayTile(), expected.tile.toArrayTile())
    }

    it("should read in a cropped SinglebandGeoTiff in the middle") {
      val local = FileRangeReader(bigPath)
      val reader = StreamingByteReader(local, chunkSize)
      val tiffTags = TiffTags.read(smallPath)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin + 100 , extent.ymin + 100, extent.xmax - 250, extent.ymax - 250)

      val actual = SinglebandGeoTiff(reader, e)
      val expected = SinglebandGeoTiff(smallPath, e)

      assertEqual(actual.tile.toArrayTile(), expected.tile.toArrayTile())
    }

    it("should read in the entire MultibandGeoTiff") {
      val local = FileRangeReader(bigPathMulti)
      val reader = StreamingByteReader(local, chunkSize)
      val actual = MultibandGeoTiff(reader)
      val expected = MultibandGeoTiff(smallPathMulti)

      assertEqual(actual.tile.toArrayTile(), expected.tile.toArrayTile())
    }

    it("should read in a cropped MultibandGeoTiff from the edge") {
      val local = FileRangeReader(bigPathMulti)
      val reader = StreamingByteReader(local, chunkSize)
      val tiffTags = TiffTags.read(smallPathMulti)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin, extent.ymin, extent.xmin + 100, extent.ymin + 100)

      val actual = MultibandGeoTiff(reader, e)
      val expected = MultibandGeoTiff(smallPathMulti, e)

      assertEqual(actual.tile.toArrayTile(), expected.tile.toArrayTile())
    }

    it("should read in a cropped MultibandGeoTiff in the middle") {
      val local = FileRangeReader(bigPathMulti)
      val reader = StreamingByteReader(local, chunkSize)
      val tiffTags = TiffTags.read(smallPathMulti)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin + 100 , extent.ymin + 100, extent.xmax - 250, extent.ymax - 250)

      val actual = MultibandGeoTiff(reader, e)
      val expected = MultibandGeoTiff(smallPathMulti, e)

      assertEqual(actual.tile.toArrayTile(), expected.tile.toArrayTile())
    }

    it("should read a previously problematic big tiff") {
      val tags = TiffTags.read(geoTiffPath("bigtiff-marcuswr.tif"))
      val e = tags.extent
      e should be (Extent(-105.06398320198056, 40.743636546229, -105.05724549293515, 40.751667086819424))
    }
  }

  describe("Writing BigTiffs") {
    val bigTiffPermutations = Table(
      ("cloud optimized", "storage method"),
      (true, Striped()),
      (true, Tiled()),
      (false, Striped()),
      (false, Tiled()),
    )

    it("should produce BigTiffs") {
      forAll(bigTiffPermutations) { (cloudOptimized, storageMethod) =>
        val tiffOriginal = MultibandGeoTiff(geoTiffPath("overviews/multiband.tif"))
        tiffOriginal.options.storageMethod shouldBe a [Striped]
        tiffOriginal.options.tiffType should be (Tiff)

        val tempFile = File.createTempFile("bigtiff", ".tif").toString
        addToPurge(tempFile)

        val bigTiff = tiffOriginal.withTiffType(BigTiff).withStorageMethod(storageMethod)

        GeoTiffWriter.write(bigTiff, tempFile, optimizedOrder = cloudOptimized)

        val actual = MultibandGeoTiff(tempFile)
        actual.options.tiffType should be (BigTiff)
        actual.options.storageMethod.getClass should be (storageMethod.getClass)
        actual.getOverviewsCount should be (5)
      }
    }

    it("should handle offsets greater than 2^32 without overflowing") {
      val tempFile = File.createTempFile("bigtiff_", ".tif")
      addToPurge(tempFile.toString)

      // the overview having a finer resolution than the full res image does not make sense but the point is that
      // the overview already tips the file size over 2^32 and the full res image's offsets should go beyond that
      // without overflowing [in the case of a cloud-optimized file layout]
      val overview = geoTiffData(cols = 32768, rows = 32768, subfileType = ReducedImage, overviews = Nil) // over 2^32
      val fullRes = geoTiffData(cols = 256, rows = 256, subfileType = FullResolutionImage, overviews = List(overview))
      GeoTiffWriter.write(fullRes, path = tempFile.toString, optimizedOrder = true)

      val bigTiffSizeThreshold = math.pow(2, 32).toLong

      assert(tempFile.length() > bigTiffSizeThreshold)

      val Array(firstFullResTileOffset, _*) = firstTileOffsets(tempFile)
      firstFullResTileOffset should be > bigTiffSizeThreshold
    }
  }

  private def firstTileOffsets(tiff: File): Array[Long] = {
    import sys.process._

    val cmd = Seq("tiffdump", tiff.toString)
    val output = cmd!!

    val tagValue = raw"<(.+)>".r.unanchored

    output.split("\n")
      .filter(_ startsWith "TileOffsets")
      .map { line =>
        val firstTileOffset = line match {
          case tagValue(values) => values.split(" ").head
        }
        firstTileOffset.toLong
      }
  }

  private def geoTiffData(cols: Int, rows: Int, subfileType: NewSubfileType, overviews: List[GeoTiffData]): GeoTiffData = {
    val (_cols, _rows) = (cols, rows)
    val _overviews = overviews
    val _cellType = IntConstantNoDataCellType

    new GeoTiffData {
      override val cellType: CellType = _cellType

      override val extent: Extent = Extent(-180, -90, 180, 90)

      override val crs: CRS = LatLng

      override val tags: Tags = Tags.empty

      override val options: GeoTiffOptions = GeoTiffOptions.DEFAULT
        .copy(tiffType = BigTiff, subfileType = Some(subfileType))

      override val overviews: List[GeoTiffData] = _overviews

      override val imageData: GeoTiffImageData = new GeoTiffImageData {
        private val (blockCols, blockRows) = (256, 256)

        override val cols: Int = _cols

        override val rows: Int = _rows

        override val bandType: BandType = BandType.forCellType(_cellType)

        override val bandCount: Int = 1

        override val segmentBytes: SegmentBytes = new SegmentBytes {
          private lazy val segment = (for {
            _ <- 0 until (blockCols * blockRows)
            byte <- Array[Byte](0, 0, 0, 123)
          } yield byte).toArray

          override def getSegment(i: Int): Array[Byte] = segment

          override def getSegments(indices: Traversable[Int]): Iterator[(Int, Array[Byte])] =
            indices.iterator.map(i => i -> getSegment(i))

          override def getSegmentByteCount(i: Int): Int = blockCols * blockRows * cellType.bytes

          override def length: Int = (cols / blockCols) * (rows / blockRows) // # of blocks
        }

        override val decompressor: Decompressor = NoCompressor

        override val segmentLayout: GeoTiffSegmentLayout = {
          GeoTiffSegmentLayout(
            totalCols = cols,
            totalRows = rows,
            tileLayout = TileLayout(layoutCols = cols / blockCols, layoutRows = rows / blockRows, tileCols = blockCols, tileRows = blockRows),
            storageMethod = Tiled(blockCols = blockCols, blockRows = blockRows),
            interleaveMethod = BandInterleave,
          )
        }
      }
    }
  }
}
