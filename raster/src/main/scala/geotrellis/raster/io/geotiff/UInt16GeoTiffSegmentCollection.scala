package geotrellis.raster.io.geotiff

import geotrellis.raster._

trait UInt16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UInt16GeoTiffSegment

  val bandType = UInt16BandType
  val cellType = TypeUShort

  val noDataValue: Option[Double]

  val createSegment: Int => UInt16GeoTiffSegment =
    noDataValue match {
      case Some(nd) if isData(nd) && Int.MinValue.toDouble <= nd && nd <= Int.MaxValue.toDouble =>
        { i: Int => new NoDataUInt16GeoTiffSegment(getDecompressedBytes(i), nd.toInt) }
      case _ =>
        { i: Int => new UInt16GeoTiffSegment(getDecompressedBytes(i)) }
    }
}
