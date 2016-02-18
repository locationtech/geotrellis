package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait UByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UByteGeoTiffSegment

  def noDataValue: Option[Int]
  val bandType = UByteBandType

  lazy val createSegment: Int => UByteGeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new UByteRawGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) if (nd == 0.toShort) =>
      { i: Int => new UByteConstantNoDataCellTypeGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) => // Cast nodata to int in this case so that we can properly compare it to the upcast unsigned byte
      { i: Int => new UByteUserDefinedNoDataGeoTiffSegment(getDecompressedBytes(i), nd.toByte) }
    }
}
