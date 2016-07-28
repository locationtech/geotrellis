package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class Float32GeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: FloatCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with CroppedGeoTiff with Float32GeoTiffSegmentCollection {

  val noDataValue: Option[Float] = cellType match {
    case FloatCellType => None
    case FloatConstantNoDataCellType => Some(Float.NaN)
    case FloatUserDefinedNoDataCellType(nd) => Some(nd)
  }

  /**
   * Reads the data out of a [[GeoTiffTile]] and create a FloatArrayTile.
   *
   * @param: CroppedGeoTiff The [[WindowedGeoTiff]] of the file
   *
   * @return A [[FloatArrayTile]]
   */
  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * FloatConstantNoDataCellType.bytes)

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
        val width = segmentTransform.segmentCols * FloatConstantNoDataCellType.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * FloatConstantNoDataCellType.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / FloatConstantNoDataCellType.bytes)
          val row = segmentTransform.indexToRow(i / FloatConstantNoDataCellType.bytes)
          val j = ((row * cols) + col) * FloatConstantNoDataCellType.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }
    FloatArrayTile.fromBytes(arr, cols, rows, cellType)
  }

  /**
   * Reads a windowed area out of a [[GeoTiffTile]] and create a FloatArrayTile.
   *
   * @param: CroppedGeoTiff The [[WindowedGeoTiff]] of the file
   *
   * @return A [[FloatArrayTile]] that conatins data from the windowed area
   */
  def crop(gridBounds: GridBounds): MutableArrayTile = {
    implicit val gb = gridBounds
    implicit val segLayout = segmentLayout
    
    val arr = Array.ofDim[Byte](gridBounds.size * FloatConstantNoDataCellType.bytes)
    val adjWidth = width * FloatConstantNoDataCellType.bytes
    val adjCols = cols * FloatConstantNoDataCellType.bytes
    var counter = 0

    if (segmentLayout.isStriped) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        implicit val segmentId = i

        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)

          val adjStart = start * FloatConstantNoDataCellType.bytes
          val adjEnd = end * FloatConstantNoDataCellType.bytes

          cfor(adjStart)(_ < adjEnd, _ + adjCols) { i =>
            val col = segmentTransform.indexToCol(i / FloatConstantNoDataCellType.bytes)
            val row = segmentTransform.indexToRow(i / FloatConstantNoDataCellType.bytes)
            if (gridBounds.contains(col, row)) {
              System.arraycopy(segment.bytes, i, arr, counter, adjWidth)
              counter += adjWidth
            }
          }
        }
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        implicit val segmentId = i

        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)
          val adjTileWidth = tileWidth * FloatConstantNoDataCellType.bytes

          val adjStart = start * FloatConstantNoDataCellType.bytes
          val adjDiff = diff * FloatConstantNoDataCellType.bytes

          cfor(adjStart)(_ < adjTileWidth * segmentRows, _ + adjTileWidth) { i =>
            val col = segmentTransform.indexToCol(i / FloatConstantNoDataCellType.bytes)
            val row = segmentTransform.indexToRow(i / FloatConstantNoDataCellType.bytes)
            if (gridBounds.contains(col, row)) {
              val j = (row - rowMin) * width + (col - colMin)
              System.arraycopy(segment.bytes, i, arr, j * FloatConstantNoDataCellType.bytes, adjDiff)
            }
          }
        }
      }
    }
    FloatArrayTile.fromBytes(arr, width, height, cellType)
  }
}
