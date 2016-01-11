package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait ByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = ByteGeoTiffSegment

  val bandType = ByteBandType
  val noDataValue: Double

  val createSegment: Int => ByteGeoTiffSegment =
    { i: Int => new ByteGeoTiffSegment(getDecompressedBytes(i), noDataValue) }
}

trait RawByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = RawByteGeoTiffSegment

  val bandType = ByteBandType

  val createSegment: Int => RawByteGeoTiffSegment =
    { i: Int => new RawByteGeoTiffSegment(getDecompressedBytes(i)) }
}
