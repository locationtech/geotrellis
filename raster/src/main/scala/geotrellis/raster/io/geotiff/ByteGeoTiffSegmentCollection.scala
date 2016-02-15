package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait ByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = ByteGeoTiffSegment

  val bandType = ByteBandType
  val noDataValue: Option[Byte]

  lazy val createSegment: Int => ByteGeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new ByteRawGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) if (nd == Short.MinValue) =>
      { i: Int => new ByteConstantNoDataCellTypeGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) =>
      { i: Int => new ByteUserDefinedNoDataGeoTiffSegment(getDecompressedBytes(i), nd) }
    }
}
