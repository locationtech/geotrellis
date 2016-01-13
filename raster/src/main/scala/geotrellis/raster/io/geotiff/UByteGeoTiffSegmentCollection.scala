package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait UByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UByteGeoTiffSegment

  val bandType = UByteBandType
  val noDataValue: Option[Int]

  val createSegment: Int => UByteGeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new UByteGeoTiffSegment(getDecompressedBytes(i)) with UByteRawSegment }
    case Some(nd) if (nd == 0.toShort) =>
      { i: Int => new UByteGeoTiffSegment(getDecompressedBytes(i)) with UByteConstantNoDataSegment }
    case Some(nd) => // Cast nodata to int in this case so that we can properly compare it to the upcast unsigned byte
      { i: Int => new UByteGeoTiffSegment(getDecompressedBytes(i)) with UByteUserDefinedNoDataSegment { val userDefinedIntNoDataValue = nd.toInt } }
    }
}
