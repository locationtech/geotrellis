package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait ByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = ByteGeoTiffSegment

  val bandType = ByteBandType
  val cellType = TypeByte

  val noDataValue: Option[Double]

  val createSegment: Int => ByteGeoTiffSegment =
    { i: Int => new ByteGeoTiffSegment(getDecompressedBytes(i)) }
}
