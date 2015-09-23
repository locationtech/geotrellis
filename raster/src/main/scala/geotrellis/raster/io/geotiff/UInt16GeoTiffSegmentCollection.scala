package geotrellis.raster.io.geotiff

import geotrellis.raster._

trait UInt16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UInt16GeoTiffSegment

  val bandType = UInt16BandType
  val cellType = TypeUShort

  val noDataValue: Option[Double]

  val createSegment: Int => UInt16GeoTiffSegment =
    { i: Int => new UInt16GeoTiffSegment(getDecompressedBytes(i)) }
}
