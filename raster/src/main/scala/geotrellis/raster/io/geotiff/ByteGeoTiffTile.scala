package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class ByteGeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: ByteCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with ByteGeoTiffSegmentCollection {

  val noDataValue: Option[Byte] = cellType match {
    case ByteCellType => None
    case ByteConstantNoDataCellType => Some(Byte.MinValue)
    case ByteUserDefinedNoDataCellType(nd) => Some(nd)
  }

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
    ByteArrayTile.fromBytes(arr, cols, rows, cellType)
  }
  
  def crop(gridBounds: GridBounds): MutableArrayTile = {
    val arr = Array.ofDim[Byte](gridBounds.size)
    var counter = 0
    
    if (segmentLayout.isStriped) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        val segmentGridBounds = segmentLayout.getGridBounds(i)
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)

          val result =
            gridBounds.intersection(segmentGridBounds).get
          val intersection =
            Intersection(segmentGridBounds, result, segmentLayout)

          cfor(intersection.start)(_ < intersection.end, _ + cols) { i =>
            System.arraycopy(segment.bytes, i, arr, counter, result.width)
            counter += result.width
          }
        }
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) {i =>
        val segmentGridBounds = segmentLayout.getGridBounds(i)
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)
          val segmentTransform = segmentLayout.getSegmentTransform(i)

          val result = gridBounds.intersection(segmentGridBounds).get
          val intersection = Intersection(segmentGridBounds, result, segmentLayout)

          cfor(intersection.start)(_ < intersection.end, _ + intersection.tileWidth) { i =>
            val col = segmentTransform.indexToCol(i)
            val row = segmentTransform.indexToRow(i)
            val j = (row - gridBounds.rowMin) * gridBounds.width + (col - gridBounds.colMin)
            System.arraycopy(segment.bytes, i, arr, j, result.width)
          }
        }
      }
    }
    ByteArrayTile.fromBytes(arr, gridBounds.width, gridBounds.height, cellType)
  }


  def withNoData(noDataValue: Option[Double]): ByteGeoTiffTile =
    new ByteGeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): Tile = {
    newCellType match {
      case dt: ByteCells with NoDataHandling =>
        new ByteGeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
