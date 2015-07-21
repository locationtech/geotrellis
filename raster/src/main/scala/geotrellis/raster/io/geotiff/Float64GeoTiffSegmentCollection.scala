package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Float64GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Float64GeoTiffSegment

  val bandType = Float64BandType
  val cellType = TypeDouble

  val noDataValue: Option[Double]

  val createSegment: Int => Float64GeoTiffSegment =
    noDataValue match {
      case Some(nd) if isData(nd) =>
        { i: Int => new NoDataFloat64GeoTiffSegment(getDecompressedBytes(i), nd) }
      case _ =>
        { i: Int => new Float64GeoTiffSegment(getDecompressedBytes(i)) }
    }
}
