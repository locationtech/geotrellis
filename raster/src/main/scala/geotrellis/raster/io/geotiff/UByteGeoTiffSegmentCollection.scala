package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait UByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UByteGeoTiffSegment

  val bandType = UByteBandType
  val cellType = TypeUByte

  val noDataValue: Double

  val createSegment: Int => UByteGeoTiffSegment =
    { i: Int => new UByteGeoTiffSegment(getDecompressedBytes(i), noDataValue.toByte) }
}

trait RawUByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = RawUByteGeoTiffSegment

  val bandType = UByteBandType
  val cellType = TypeRawUByte

  val createSegment: Int => RawUByteGeoTiffSegment =
    { i: Int => new RawUByteGeoTiffSegment(getDecompressedBytes(i)) }
}
