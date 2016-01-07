package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait UByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UByteGeoTiffSegment

  val bandType = UByteBandType
  val cellType = TypeUByte

  val noDataValue: Option[Double]

  val createSegment: Int => UByteGeoTiffSegment = noDataValue match {
    case Some(nd) =>
      { i: Int => new UByteGeoTiffSegment(getDecompressedBytes(i), nd.toByte) }
    case None =>
      { i: Int => new UByteGeoTiffSegment(getDecompressedBytes(i), 0.toByte) }
    }
}

trait RawUByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = RawUByteGeoTiffSegment

  val bandType = UByteBandType
  val cellType = TypeRawUByte

  val createSegment: Int => RawUByteGeoTiffSegment =
    { i: Int => new RawUByteGeoTiffSegment(getDecompressedBytes(i)) }
}
