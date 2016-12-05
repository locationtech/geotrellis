package geotrellis.raster.io.geotiff

import geotrellis.util._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.testkit._

import monocle.syntax.apply._
import spire.syntax.cfor._
import org.scalatest._

class StreamingSegmentBytesSpec extends FunSpec
  with Matchers
	with GeoTiffTestUtils {

	describe("Testing the StreamingSegmentBytes") {
		describe("Striped Storage") {
			val path = geoTiffPath("ls8_int32.tif")
			val fileReader = FileRangeReader(path)

			val reader = StreamingByteReader(fileReader)
			val tiffTags = TiffTagsReader.read(reader)

			val storageMethod: StorageMethod =
				if(tiffTags.hasStripStorage) {
					val rowsPerStrip: Int =
						(tiffTags
							&|-> TiffTags._basicTags
							^|-> BasicTags._rowsPerStrip get).toInt

					Striped(rowsPerStrip)
				} else {
					val blockCols =
						(tiffTags
							&|-> TiffTags._tileTags
							^|-> TileTags._tileWidth get).get.toInt

					val blockRows =
						(tiffTags
							&|-> TiffTags._tileTags
							^|-> TileTags._tileLength get).get.toInt

					Tiled(blockCols, blockRows)
				}

				val segmentLayout =
					GeoTiffSegmentLayout(tiffTags.cols, tiffTags.rows, storageMethod, tiffTags.bandType)

			it("Should return all of the segments if the whole file is selected") {
				val streaming =
					new StreamingSegmentBytes(reader, segmentLayout, None, tiffTags)

				streaming.intersectingSegments.length should be (tiffTags.segmentCount)
				streaming.remainingSegments.length should be (0)
			}

			it("Should return segments that intersect the gridbounds, beginning") {
				val rowsPerStrip = tiffTags.basicTags.rowsPerStrip.toInt

				val colMin = tiffTags.cols / 6
				val rowMin = 0
				val colMax = tiffTags.cols / 2
				val rowMax = rowsPerStrip * 10 + rowMin

				val gridBounds = GridBounds(colMin, rowMin, colMax, rowMax)

				val rasterExtent = RasterExtent(tiffTags.extent, tiffTags.cols, tiffTags.rows)
				val extent = rasterExtent.rasterExtentFor(gridBounds).extent

				val streaming =
					new StreamingSegmentBytes(reader, segmentLayout, Some(extent), tiffTags)

				val actual: Array[Int] = {
					val coords = gridBounds.coords
					val set = scala.collection.mutable.Set[Int]()

					cfor(0)(_ < coords.length, _ + 1){ i =>
						set += segmentLayout.getSegmentIndex(coords(i)._1, coords(i)._2)
					}

					set.toArray
				}

				val expected = streaming.intersectingSegments

				expected should be (actual.sortWith(_ < _))
			}
			
			it("Should return segments that intersect the gridbounds, middle") {
				val rowsPerStrip = tiffTags.basicTags.rowsPerStrip.toInt

				val colMin = tiffTags.cols / 4
				val rowMin = tiffTags.rows / 2
				val colMax = tiffTags.cols / 2
				val rowMax = tiffTags.rows - 1

				val gridBounds = GridBounds(colMin, rowMin, colMax, rowMax)

				val rasterExtent = RasterExtent(tiffTags.extent, tiffTags.cols, tiffTags.rows)
				val extent = rasterExtent.rasterExtentFor(gridBounds).extent

				val streaming =
					new StreamingSegmentBytes(reader, segmentLayout, Some(extent), tiffTags)
				
				val actual: Array[Int] = {
					val coords = gridBounds.coords
					val set = scala.collection.mutable.Set[Int]()

					cfor(0)(_ < coords.length, _ + 1){ i =>
						set += segmentLayout.getSegmentIndex(coords(i)._1, coords(i)._2)
					}

					set.toArray
				}

				val expected = streaming.intersectingSegments

				expected should be (actual.sortWith(_ < _))
			}
		}
		
		describe("Tiled Storage") {
			val path = geoTiffPath("bilevel_tiled.tif")
			val fileReader = FileRangeReader(path)

			val reader = StreamingByteReader(fileReader)
			val tiffTags = TiffTagsReader.read(reader)

			val storageMethod: StorageMethod =
				if(tiffTags.hasStripStorage) {
					val rowsPerStrip: Int =
						(tiffTags
							&|-> TiffTags._basicTags
							^|-> BasicTags._rowsPerStrip get).toInt

					Striped(rowsPerStrip)
				} else {
					val blockCols =
						(tiffTags
							&|-> TiffTags._tileTags
							^|-> TileTags._tileWidth get).get.toInt

					val blockRows =
						(tiffTags
							&|-> TiffTags._tileTags
							^|-> TileTags._tileLength get).get.toInt

					Tiled(blockCols, blockRows)
				}

				val segmentLayout =
					GeoTiffSegmentLayout(tiffTags.cols, tiffTags.rows, storageMethod, tiffTags.bandType)

			it("Should return all of the segments if the whole file is selected") {
				val streaming =
					new StreamingSegmentBytes(reader, segmentLayout, None, tiffTags)

				streaming.intersectingSegments.length should be (tiffTags.segmentCount)
				streaming.remainingSegments.length should be (0)
			}
			
			it("Should return segments that intersect the gridbounds, beginning") {
				val colsPerTile = tiffTags.tileTags.tileWidth.get.toInt
				val rowsPerTile = tiffTags.tileTags.tileLength.get.toInt

				val colMin = tiffTags.cols / 6
				val rowMin = rowsPerTile / 2
				val colMax = tiffTags.cols / 2
				val rowMax = rowsPerTile * 3 + rowMin

				val gridBounds = GridBounds(colMin, rowMin, colMax, rowMax)

				val rasterExtent = RasterExtent(tiffTags.extent, tiffTags.cols, tiffTags.rows)
				val extent = rasterExtent.rasterExtentFor(gridBounds).extent

				val streaming =
					new StreamingSegmentBytes(reader, segmentLayout, Some(extent), tiffTags)

				val actual: Array[Int] = {
					val coords = gridBounds.coords
					val set = scala.collection.mutable.Set[Int]()

					cfor(0)(_ < coords.length, _ + 1){ i =>
						set += segmentLayout.getSegmentIndex(coords(i)._1, coords(i)._2)
					}

					set.toArray
				}

				val expected = streaming.intersectingSegments

				expected should be (actual.sortWith(_ < _))
				streaming.remainingSegments.length should be (streaming.size - expected.length)
			}
			
			it("Should return segments that intersect the gridbounds, middle") {
				val rowsPerStrip = tiffTags.basicTags.rowsPerStrip.toInt

				val colMin = tiffTags.cols / 4
				val rowMin = tiffTags.rows / 2
				val colMax = tiffTags.cols / 2
				val rowMax = tiffTags.rows

				val gridBounds = GridBounds(colMin, rowMin, colMax, rowMax)

				val rasterExtent = RasterExtent(tiffTags.extent, tiffTags.cols, tiffTags.rows)
				val extent = rasterExtent.rasterExtentFor(gridBounds).extent

				val streaming =
					new StreamingSegmentBytes(reader, segmentLayout, Some(extent), tiffTags)
				
				val actual: Array[Int] = {
					val coords = gridBounds.coords
					val set = scala.collection.mutable.Set[Int]()

					cfor(0)(_ < coords.length, _ + 1){ i =>
						set += segmentLayout.getSegmentIndex(coords(i)._1, coords(i)._2)
					}

					set.toArray
				}

				val expected = streaming.intersectingSegments

				expected should be (actual.sortWith(_ < _))
			}
		}
	}
}
