package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class Int32GeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  val bandType = Int32BandType

  // Cached last segment
  private var _lastSegment: Int32GeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => Int32GeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) && Int.MinValue.toDouble <= nd && nd <= Int.MaxValue.toDouble =>
        { i: Int => new NoDataInt32GeoTiffSegment(getDecompressedBytes(i), nd.toInt) }
      case _ =>
        { i: Int => new Int32GeoTiffSegment(getDecompressedBytes(i)) }
    }

  val cellType = TypeInt

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * TypeInt.bytes)
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
        val width = segmentTransform.segmentCols * TypeInt.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * TypeInt.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / TypeInt.bytes)
          val row = segmentTransform.indexToRow(i / TypeInt.bytes)
          val j = ((row * cols) + col) * TypeInt.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }

    noDataValue match {
      case Some(nd) if isData(nd) && Int.MinValue.toDouble <= nd && nd <= Int.MaxValue.toDouble =>
        IntArrayTile.fromBytes(arr, cols, rows, nd.toInt)
      case _ =>
        IntArrayTile.fromBytes(arr, cols, rows)
    }
  }
}
