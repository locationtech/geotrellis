package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Float64GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Float64GeoTiffSegment

  val bandType = Float64BandType
  val noDataValue: Option[Double]

  lazy val createSegment: Int => Float64GeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new Float64RawGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) if (nd == Float.NaN) =>
      { i: Int => new Float64ConstantNoDataGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) =>
      { i: Int => new Float64UserDefinedNoDataGeoTiffSegment(getDecompressedBytes(i), nd) }
  }
}
