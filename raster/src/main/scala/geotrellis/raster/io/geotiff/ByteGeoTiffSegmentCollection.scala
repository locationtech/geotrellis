package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait ByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = ByteGeoTiffSegment

  val cellType = TypeDynamicByte
  val bandType = ByteBandType

  val noDataValue: Option[Double]

  val createSegment: Int => ByteGeoTiffSegment = noDataValue match {
    case Some(nd) if (isData(nd) && Byte.MinValue.toDouble <= nd && nd <= Byte.MaxValue.toDouble) =>
      { i: Int => new ByteGeoTiffSegment(getDecompressedBytes(i), nd.toByte) }
    case None =>
      { i: Int => new ByteGeoTiffSegment(getDecompressedBytes(i), byteNODATA) }
  }
}

trait RawByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = RawByteGeoTiffSegment

  val cellType = TypeRawByte
  val bandType = ByteBandType

  val createSegment: Int => RawByteGeoTiffSegment =
    { i: Int => new RawByteGeoTiffSegment(getDecompressedBytes(i)) }
}
