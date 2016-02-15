package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Float32GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Float32GeoTiffSegment

  val bandType = Float32BandType
  val noDataValue: Option[Float]

  lazy val createSegment: Int => Float32GeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new Float32RawGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) if (nd == Float.NaN) =>
      { i: Int => new Float32ConstantNoDataGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) =>
      { i: Int => new Float32UserDefinedNoDataGeoTiffSegment(getDecompressedBytes(i), nd) }
  }
}
