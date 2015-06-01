package geotrellis.raster.io.geotiff

import geotrellis.raster._

trait BitGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = BitGeoTiffSegment

  val bandType = BitBandType
  val cellType = TypeBit

  val segmentLayout: GeoTiffSegmentLayout

  val bandCount: Int
  val hasPixelInterleave: Boolean

  val createSegment: Int => BitGeoTiffSegment = { i =>
    val (segmentCols, segmentRows) = segmentLayout.getSegmentDimensions(i)
    // val size = segmentCols * segmentRows
    val cols = {
      val c = segmentLayout.tileLayout.tileCols
      if(hasPixelInterleave) c * bandCount
      else c
    }

    val rows = if(segmentLayout.isStriped) { segmentRows } else { segmentLayout.tileLayout.tileRows }

    new BitGeoTiffSegment(getDecompressedBytes(i), cols, rows)
  }

}
