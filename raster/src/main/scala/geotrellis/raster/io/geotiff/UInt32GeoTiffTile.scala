package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class UInt32GeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  val bandType = UInt32BandType

  // Cached last segment
  private var _lastSegment: UInt32GeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => UInt32GeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) =>
        { i: Int => new NoDataUInt32GeoTiffSegment(getDecompressedBytes(i), nd.toFloat) }
      case _ =>
        { i: Int => new UInt32GeoTiffSegment(getDecompressedBytes(i)) }
    }

  val cellType = TypeFloat

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Float](cols * rows)
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
    FloatArrayTile(arr, cols, rows)
  }
}
