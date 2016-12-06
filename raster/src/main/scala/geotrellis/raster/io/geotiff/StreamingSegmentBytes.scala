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

	lazy val intersectingSegments: Array[Int] = {
		if (extent != tiffTags.extent) {
			val array = scala.collection.mutable.ArrayBuffer[Int]()

			for (i <- (0 until tiffTags.segmentCount)) {
				val segmentTransform = segmentLayout.getSegmentTransform(i)

				val startCol: Int = segmentTransform.indexToCol(0)
				val startRow: Int = segmentTransform.indexToRow(0)
				val endCol: Int = startCol + segmentTransform.segmentCols
				val endRow: Int = startRow + segmentTransform.segmentRows

				val start = (!(startCol > colMax) && !(startRow > rowMax))
				val end = (!(endCol <= colMin) && !(endRow <= rowMin))

				if (start && end)
					array += i
			}
			
			array.toArray
		} else {
			Array.range(0, tiffTags.segmentCount)
		}
	}

	val (offsets, byteCounts) =
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
	
	private lazy val compressedBytes: Array[Array[Byte]] = {
		val result = Array.ofDim[Array[Byte]](offsets.length)
				
		cfor(0)(_ < intersectingSegments.size, _ + 1) { i =>
			val value = intersectingSegments(i)
			println(s"value = $value")
			result(value) = byteReader.getSignedByteArray(byteCounts(value), offsets(value))
		}
		result
	}

	override val size = offsets.size

	def getSegment(i: Int): Array[Byte] =
		if (intersectingSegments.contains(i))
			compressedBytes(i)
		else {
			println(s"$i WASN'T IN THE COMPRESSED BYTES")
			byteReader.getSignedByteArray(byteCounts(i), offsets(i))
		}
}

object StreamingSegmentBytes {

	def apply(byteReader: ByteReader, segmentLayout: GeoTiffSegmentLayout,
		extent: Option[Extent], tiffTags: TiffTags): StreamingSegmentBytes =
			new StreamingSegmentBytes(byteReader, segmentLayout, extent, tiffTags)
}
