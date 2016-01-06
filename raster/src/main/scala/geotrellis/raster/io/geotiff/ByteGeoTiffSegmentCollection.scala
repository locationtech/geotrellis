package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait ByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = ByteGeoTiffSegment

  val cellType = TypeByte
  val bandType = ByteBandType

  val noDataValue: Double

  val createSegment: Int => ByteGeoTiffSegment =
    if (isData(noDataValue) && Byte.MinValue.toDouble <= noDataValue && noDataValue <= Byte.MaxValue.toDouble)
      { i: Int => new ByteGeoTiffSegment(getDecompressedBytes(i), noDataValue.toByte) }
    else
      { i: Int => new ByteGeoTiffSegment(getDecompressedBytes(i), byteNODATA) }
}

trait RawByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = RawByteGeoTiffSegment

  val cellType = TypeRawByte
  val bandType = ByteBandType

  val createSegment: Int => RawByteGeoTiffSegment =
    { i: Int => new RawByteGeoTiffSegment(getDecompressedBytes(i)) }
}
