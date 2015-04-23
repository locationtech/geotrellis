package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class Float64GeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  // Cached last segment
  private var _lastSegment: Float64GeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => Float64GeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) =>
        { i: Int => new NoDataFloat64GeoTiffSegment(getDecompressedBytes(i), nd) }
      case _ =>
        { i: Int => new Float64GeoTiffSegment(getDecompressedBytes(i)) }
    }

  val cellType = TypeDouble

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * TypeDouble.bytes)

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
        val width = segmentTransform.segmentCols * TypeDouble.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * TypeDouble.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / TypeDouble.bytes)
          val row = segmentTransform.indexToRow(i / TypeDouble.bytes)
          val j = ((row * cols) + col) * TypeDouble.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }
    noDataValue match {
      case Some(nd) if isData(nd) =>
        DoubleArrayTile.fromBytes(arr, cols, rows, nd)
      case _ =>
        DoubleArrayTile.fromBytes(arr, cols, rows)
    }
  }
}
