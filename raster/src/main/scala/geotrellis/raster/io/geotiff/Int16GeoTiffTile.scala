package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class Int16GeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  val bandType = Int16BandType

  // Cached last segment
  private var _lastSegment: Int16GeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => Int16GeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) && Short.MinValue.toDouble <= nd && nd <= Short.MaxValue.toDouble =>
        { i: Int => new NoDataInt16GeoTiffSegment(getDecompressedBytes(i), nd.toShort) }
      case _ =>
        { i: Int => new Int16GeoTiffSegment(getDecompressedBytes(i)) }
    }

  val cellType = TypeShort

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * TypeShort.bytes)

    if(segmentLayout.isStriped) {
      var i = 0
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)
        val size = segment.bytes.size
        System.arraycopy(segment.bytes, 0, arr, i, size)
        i += size
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)

        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val width = segmentTransform.segmentCols * TypeShort.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * TypeShort.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / TypeShort.bytes)
          val row = segmentTransform.indexToRow(i / TypeShort.bytes)
          val j = ((row * cols) + col) * TypeShort.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }

    noDataValue match {
      case Some(nd) if isData(nd) && Short.MinValue.toDouble <= nd && nd <= Short.MaxValue.toDouble =>
        ShortArrayTile.fromBytes(arr, cols, rows, nd.toShort)
      case _ =>
        ShortArrayTile.fromBytes(arr, cols, rows)
    }
  }
}
