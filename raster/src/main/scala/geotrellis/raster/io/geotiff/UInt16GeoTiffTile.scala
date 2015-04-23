package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class UInt16GeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  // Cached last segment
  private var _lastSegment: UInt16GeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => UInt16GeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) && Int.MinValue.toDouble <= nd && nd <= Int.MaxValue.toDouble =>
        { i: Int => new NoDataUInt16GeoTiffSegment(getDecompressedBytes(i), nd.toInt) }
      case _ =>
        { i: Int => new UInt16GeoTiffSegment(getDecompressedBytes(i)) }
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
    val arr = Array.ofDim[Int](cols * rows)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = 
        if(segmentIndex == _lastSegmentIndex) _lastSegment
        else createSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      cfor(0)(_ < segment.size, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          arr(row * cols + col) = segment.get(i)
        }
      }
    }
    IntArrayTile(arr, cols, rows)
  }
}
