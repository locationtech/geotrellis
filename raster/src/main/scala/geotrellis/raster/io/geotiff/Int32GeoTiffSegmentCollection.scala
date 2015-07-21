package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Int32GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Int32GeoTiffSegment

  val bandType = Int32BandType
  val cellType = TypeInt

  val noDataValue: Option[Double]

  val createSegment: Int => Int32GeoTiffSegment =
    noDataValue match {
      case Some(nd) if isData(nd) && Int.MinValue.toDouble <= nd && nd <= Int.MaxValue.toDouble =>
        { i: Int => new NoDataInt32GeoTiffSegment(getDecompressedBytes(i), nd.toInt) }
      case _ =>
        { i: Int => new Int32GeoTiffSegment(getDecompressedBytes(i)) }
    }
}
