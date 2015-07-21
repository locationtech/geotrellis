package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait UInt32GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UInt32GeoTiffSegment

  val bandType = UInt32BandType
  val cellType = TypeFloat

  val noDataValue: Option[Double]

  val createSegment: Int => UInt32GeoTiffSegment =
    noDataValue match {
      case Some(nd) if isData(nd) =>
        { i: Int => new NoDataUInt32GeoTiffSegment(getDecompressedBytes(i), nd.toFloat) }
      case _ =>
        { i: Int => new UInt32GeoTiffSegment(getDecompressedBytes(i)) }
    }
}
