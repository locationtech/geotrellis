package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class BitGeoTiffTile(
  compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  val bandType = BitBandType

  // Cached last segment
  private var _lastSegment: BitGeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private def createSegment(i: Int): BitGeoTiffSegment = {
    val (segmentCols, segmentRows) = segmentLayout.getSegmentDimensions(i)
    val size = segmentCols * segmentRows
    val width = if(segmentLayout.isStriped) { segmentCols } else { segmentLayout.tileLayout.tileCols }
    new BitGeoTiffSegment(getDecompressedBytes(i), size, width)
  }

  val cellType = TypeBit

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  // TODO: Optimize this.
  def mutable: MutableArrayTile = {
    val result = BitArrayTile.empty(cols, rows)

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        result.set(col, row, get(col, row))
      }
    }

   result
  }
}
