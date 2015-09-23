package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait UByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UByteGeoTiffSegment

  val bandType = UByteBandType
  val cellType = TypeUByte

  val noDataValue: Option[Double]

  val createSegment: Int => UByteGeoTiffSegment =
    { i: Int => new UByteGeoTiffSegment(getDecompressedBytes(i)) }
}
