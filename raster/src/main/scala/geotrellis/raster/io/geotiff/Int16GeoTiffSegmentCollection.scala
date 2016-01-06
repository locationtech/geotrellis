package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Int16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Int16GeoTiffSegment

  val bandType = Int16BandType
  val cellType = TypeShort

  val noDataValue: Double

  val createSegment: Int => Int16GeoTiffSegment =
    { i: Int => new Int16GeoTiffSegment(getDecompressedBytes(i), noDataValue.toShort) }
}

trait RawInt16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = RawInt16GeoTiffSegment

  val bandType = Int16BandType
  val cellType = TypeRawShort

  val createSegment: Int => RawInt16GeoTiffSegment =
    { i: Int => new RawInt16GeoTiffSegment(getDecompressedBytes(i)) }
}
