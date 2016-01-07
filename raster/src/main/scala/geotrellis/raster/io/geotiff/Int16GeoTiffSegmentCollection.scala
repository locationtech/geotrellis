package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Int16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Int16GeoTiffSegment

  val bandType = Int16BandType
  val cellType = TypeShort

  val noDataValue: Option[Double]

  val createSegment: Int => Int16GeoTiffSegment = noDataValue match {
    case Some(nd) =>
      { i: Int => new Int16GeoTiffSegment(getDecompressedBytes(i), nd.toShort) }
    case None =>
      { i: Int => new Int16GeoTiffSegment(getDecompressedBytes(i), shortNODATA) }
  }
}

trait RawInt16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = RawInt16GeoTiffSegment

  val bandType = Int16BandType
  val cellType = TypeRawShort

  val createSegment: Int => RawInt16GeoTiffSegment =
    { i: Int => new RawInt16GeoTiffSegment(getDecompressedBytes(i)) }
}
