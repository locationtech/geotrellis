package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Int16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Int16GeoTiffSegment

  val bandType = Int16BandType
  val cellType = TypeShort

  val noDataValue: Option[Double]

  val createSegment: Int => Int16GeoTiffSegment =
    noDataValue match {
      case Some(nd) if isData(nd) && Short.MinValue.toDouble <= nd && nd <= Short.MaxValue.toDouble =>
        { i: Int => new NoDataInt16GeoTiffSegment(getDecompressedBytes(i), nd.toShort) }
      case _ =>
        { i: Int => new Int16GeoTiffSegment(getDecompressedBytes(i)) }
    }
}
