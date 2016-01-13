package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait ByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = ByteGeoTiffSegment

  val bandType = ByteBandType
  val noDataValue: Option[Byte]

  val createSegment: Int => ByteGeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new ByteGeoTiffSegment(getDecompressedBytes(i)) with ByteRawSegment }
    case Some(nd) if (nd == Short.MinValue) =>
      { i: Int => new ByteGeoTiffSegment(getDecompressedBytes(i)) with ByteConstantNoDataSegment }
    case Some(nd) =>
      { i: Int => new ByteGeoTiffSegment(getDecompressedBytes(i)) with ByteUserDefinedNoDataSegment { val noDataValue = nd } }
    }
}
