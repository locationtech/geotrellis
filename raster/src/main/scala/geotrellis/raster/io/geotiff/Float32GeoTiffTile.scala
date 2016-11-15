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
) extends GeoTiffTile(segmentLayout, compression) with Float32GeoTiffSegmentCollection {

  val noDataValue: Option[Float] = cellType match {
    case FloatCellType => None
    case FloatConstantNoDataCellType => Some(Float.NaN)
    case FloatUserDefinedNoDataCellType(nd) => Some(nd)
  }

  /**
   * Reads the data out of a [[GeoTiffTile]] and create a
   * FloatArrayTile.
   *
   * @param  CroppedGeoTiff  The [[WindowedGeoTiff]] of the file
   * @return                 A [[FloatArrayTile]]
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
   * Reads a windowed area out of a [[GeoTiffTile]] and create a
   * FloatArrayTile.
   *
   * @param  CroppedGeoTiff  The [[WindowedGeoTiff]] of the file
   * @return                 A [[FloatArrayTile]] that conatins data from the windowed area
   */
  def crop(gridBounds: GridBounds): MutableArrayTile = {
    val arr = Array.ofDim[Byte](gridBounds.size * FloatConstantNoDataCellType.bytes)
    var counter = 0

    if (segmentLayout.isStriped) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        val segmentGridBounds = segmentLayout.getGridBounds(i)
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)

          val result = gridBounds.intersection(segmentGridBounds).get
          val intersection = Intersection(segmentGridBounds, result, segmentLayout)

          val adjStart = intersection.start * FloatConstantNoDataCellType.bytes
          val adjEnd = intersection.end * FloatConstantNoDataCellType.bytes
          val adjCols = intersection.cols * FloatConstantNoDataCellType.bytes
          val adjWidth = result.width * FloatConstantNoDataCellType.bytes

          cfor(adjStart)(_ < adjEnd, _ + adjCols) { i =>
            System.arraycopy(segment.bytes, i, arr, counter, adjWidth)
            counter += adjWidth
          }
        }
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        val segmentGridBounds = segmentLayout.getGridBounds(i)
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)
          val segmentTransform = segmentLayout.getSegmentTransform(i)

          val result = gridBounds.intersection(segmentGridBounds).get
          val intersection = Intersection(segmentGridBounds, result, segmentLayout)

          val adjStart = intersection.start * FloatConstantNoDataCellType.bytes
          val adjEnd = intersection.end * FloatConstantNoDataCellType.bytes
          val adjTileWidth = intersection.tileWidth * FloatConstantNoDataCellType.bytes
          val adjWidth = result.width * FloatConstantNoDataCellType.bytes

          cfor(adjStart)(_ < adjEnd, _ + adjTileWidth) { i =>
            val col = segmentTransform.indexToCol(i / FloatConstantNoDataCellType.bytes)
            val row = segmentTransform.indexToRow(i / FloatConstantNoDataCellType.bytes)
            val j = (row - gridBounds.rowMin) * gridBounds.width + (col - gridBounds.colMin)
            System.arraycopy(segment.bytes, i, arr, j * FloatConstantNoDataCellType.bytes, adjWidth)
          }
        }
      }
    }
    FloatArrayTile.fromBytes(arr, gridBounds.width, gridBounds.height, cellType)
  }


  def withNoData(noDataValue: Option[Double]): Float32GeoTiffTile =
    new Float32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): Tile = {
    newCellType match {
      case dt: FloatCells with NoDataHandling =>
        new Float32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }  
}
