package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Int32GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Int32GeoTiffSegment

  val bandType = Int32BandType
  val noDataValue: Option[Int]

  lazy val createSegment: Int => Int32GeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new Int32RawGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) if (nd == Int.MinValue) =>
      { i: Int => new Int32ConstantNoDataGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) =>
      { i: Int => new Int32UserDefinedNoDataGeoTiffSegment(getDecompressedBytes(i), nd) }
  }
}
