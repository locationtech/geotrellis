package geotrellis.raster.io.geotiff

import geotrellis.util._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.util._
import geotrellis.vector.Extent

import spire.syntax.cfor._
import monocle.syntax.apply._

class StreamingSegmentBytes(byteReader: ByteReader,
	segmentLayout: GeoTiffSegmentLayout,
	croppedExtent: Option[Extent],
	tiffTags: TiffTags) extends SegmentBytes {

	private val extent: Extent =
		croppedExtent match {
			case Some(e) => e
			case None => tiffTags.extent
		}

	private lazy val gridBounds: GridBounds = {
		val rasterExtent: RasterExtent =
			RasterExtent(tiffTags.extent, tiffTags.cols, tiffTags.rows)
		rasterExtent.gridBoundsFor(extent)
	}
	
	private lazy val colMin: Int = gridBounds.colMin
	private lazy val rowMin: Int = gridBounds.rowMin
	private lazy val colMax: Int = gridBounds.colMax
	private lazy val rowMax: Int = gridBounds.rowMax

	val intersectingSegments: Array[Int] = {
		if (extent != tiffTags.extent) {
			val array = scala.collection.mutable.ArrayBuffer[Int]()

			cfor(0)(_ < tiffTags.segmentCount, _ + 1) {i =>
				val segmentTransform = segmentLayout.getSegmentTransform(i)

				val startCol: Int = segmentTransform.indexToCol(0)
				val startRow: Int = segmentTransform.indexToRow(0)
				val endCol: Int = startCol + segmentTransform.segmentCols - 1
				val endRow: Int = startRow + segmentTransform.segmentRows - 1

				assert(startCol == 0 && endCol == tiffTags.cols - 1, s"startCol = $startCol, endCol = $endCol")

				val start = (!(startCol >= colMax) && !(startRow >= rowMax))
				val end = (!(endCol <= colMin) && !(endRow <= rowMin))

				if (start && end)
					array += i
			}

			println(array.mkString(" "))
			array.toArray.sorted
		} else {
			Array.range(0, tiffTags.segmentCount)
		}
	}

	private val (offsets, byteCounts) =
		if (tiffTags.hasStripStorage) {
			val stripOffsets = (tiffTags &|->
				TiffTags._basicTags ^|->
				BasicTags._stripOffsets get)
			
			val stripByteCounts = (tiffTags &|->
				TiffTags._basicTags ^|->
				BasicTags._stripByteCounts get)

			(stripOffsets.get, stripByteCounts.get)

		} else {
			val tileOffsets = (tiffTags &|->
				TiffTags._tileTags ^|->
				TileTags._tileOffsets get)

			val tileByteCounts = (tiffTags &|->
				TiffTags._tileTags ^|->
				TileTags._tileByteCounts get)

			(tileOffsets.get, tileByteCounts.get)
		}

	private lazy val intervals: Seq[(Long, Long)] = {
		val mergeQueue = new MergeQueue(intersectingSegments.length)

			cfor(0)(_ < intersectingSegments.length, _ + 1){ i =>
			val segment = intersectingSegments(i)
			mergeQueue += (byteCounts(segment), offsets(segment))
		}
		mergeQueue.toSeq
	}

	private lazy val compressedBytes: Array[Array[Byte]] = {
		val result = Array.ofDim[Array[Byte]](intervals.size)
				
		cfor(0)(_ < intervals.size, _ + 1) { i =>
			val (startOffset, endOffset) = intervals(i)
			result(i) = byteReader.getSignedByteArray(endOffset - startOffset + 1, startOffset)
		}
		result
	}

	override val size = offsets.size

	def getSegment(i: Int): Array[Byte] =
		if (i == 0)
			throw new Error("i was 0")
		else if (intersectingSegments.contains(i)) {
			val offset = offsets(i)
			var arrayIndex: Option[Int] = None

			cfor(0)(_ < intervals.length, _ + 1){ index =>
				val range = intervals(index)
				println(s"i = $i, offset = $offset, range = $range")
				if (offset >= range._1 && offset <= range._2)
					arrayIndex = Some(index)
			}

			arrayIndex match {
				case Some(x) => compressedBytes(x)
				case None => throw new Error(s"Could not locate bytes for segment: $i")
			}
		} else {
			throw new IndexOutOfBoundsException(s"Segment $i does not intersect: $extent")
			//byteReader.getSignedByteArray(byteCounts(i), offsets(i))
		}
}

object StreamingSegmentBytes {

	def apply(byteReader: ByteReader, segmentLayout: GeoTiffSegmentLayout,
		extent: Option[Extent], tiffTags: TiffTags): StreamingSegmentBytes =
			new StreamingSegmentBytes(byteReader, segmentLayout, extent, tiffTags)
}
