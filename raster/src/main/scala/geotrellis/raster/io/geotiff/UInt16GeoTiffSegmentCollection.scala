package geotrellis.raster.io.geotiff

import geotrellis.raster._

trait UInt16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UInt16GeoTiffSegment

  val bandType = UInt16BandType
  val noDataValue: Option[Int]

  lazy val createSegment: Int => UInt16GeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new UInt16RawGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) if (nd == 0) =>
      { i: Int => new UInt16ConstantNoDataGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) => // Cast nodata to int in this case so that we can properly compare it to the upcast unsigned byte
      { i: Int => new UInt16UserDefinedNoDataGeoTiffSegment(getDecompressedBytes(i), nd.toShort) }
    }
}
