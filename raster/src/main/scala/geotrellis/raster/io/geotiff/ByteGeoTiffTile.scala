package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class ByteGeoTiffTile(
  val compressedBytes: Array[Array[Byte]],
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  cellType: DynamicCellType
) extends GeoTiffTile(segmentLayout, compression, cellType) with ByteGeoTiffSegmentCollection {
  val noDataValue = cellType.noDataValue

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows)

    if(segmentLayout.isStriped) {
      var i = 0
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          getSegment(segmentIndex)
        val size = segment.bytes.size
        System.arraycopy(segment.bytes, 0, arr, i, size)
        i += size
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          getSegment(segmentIndex)

        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val width = segmentTransform.segmentCols
        val tileWidth = segmentLayout.tileLayout.tileCols

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i)
          val row = segmentTransform.indexToRow(i)
          val j = (row * cols) + col
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }

    ByteArrayTile.fromBytes(arr, cols, rows, cellType.noDataValue.toByte)
  }
}

class RawByteGeoTiffTile(
  val compressedBytes: Array[Array[Byte]],
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression
) extends GeoTiffTile(segmentLayout, compression, TypeRawByte) with RawByteGeoTiffSegmentCollection {
  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows)

    if(segmentLayout.isStriped) {
      var i = 0
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          getSegment(segmentIndex)
        val size = segment.bytes.size
        System.arraycopy(segment.bytes, 0, arr, i, size)
        i += size
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          getSegment(segmentIndex)

        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val width = segmentTransform.segmentCols
        val tileWidth = segmentLayout.tileLayout.tileCols

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i)
          val row = segmentTransform.indexToRow(i)
          val j = (row * cols) + col
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }

    RawByteArrayTile.fromBytes(arr, cols, rows)
  }
}
