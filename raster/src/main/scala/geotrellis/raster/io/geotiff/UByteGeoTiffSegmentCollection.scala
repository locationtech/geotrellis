package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait UByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UByteGeoTiffSegment

  val bandType = UByteBandType
  val cellType = TypeUByte

  val noDataValue: Option[Double]

  val createSegment: Int => UByteGeoTiffSegment =
    noDataValue match {
      case Some(nd) if isData(nd) && Byte.MinValue.toDouble <= nd && nd <= Byte.MaxValue.toDouble =>
        { i: Int => new NoDataUByteGeoTiffSegment(getDecompressedBytes(i), nd.toByte) }
      case _ =>
        { i: Int => new UByteGeoTiffSegment(getDecompressedBytes(i)) }
    }
}
