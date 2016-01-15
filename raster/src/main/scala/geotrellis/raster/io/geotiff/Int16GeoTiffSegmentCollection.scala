package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Int16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Int16GeoTiffSegment

  val bandType = Int16BandType
  val noDataValue: Option[Short]

  lazy val createSegment: Int => Int16GeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new Int16RawGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) if (nd == Short.MinValue) =>
      { i: Int => new Int16ConstantNoDataGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) =>
      { i: Int => new Int16UserDefinedNoDataGeoTiffSegment(getDecompressedBytes(i), nd) }
  }
}
