package geotrellis.raster.io.geotiff

import geotrellis.raster._

trait BitGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = BitGeoTiffSegment

  val bandType = BitBandType
  val cellType = TypeBit

  val segmentLayout: GeoTiffSegmentLayout

  val createSegment: Int => BitGeoTiffSegment = { i =>
    val (segmentCols, segmentRows) = segmentLayout.getSegmentDimensions(i)
    val size = segmentCols * segmentRows
    val width = if(segmentLayout.isStriped) { segmentCols } else { segmentLayout.tileLayout.tileCols }
    new BitGeoTiffSegment(getDecompressedBytes(i), size, width)
  }

}
