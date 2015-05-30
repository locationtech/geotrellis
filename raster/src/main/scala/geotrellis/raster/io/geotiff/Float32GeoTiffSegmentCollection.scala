package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Float32GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Float32GeoTiffSegment

  val bandType = Float32BandType
  val cellType = TypeFloat

  val noDataValue: Option[Double]
  
  val createSegment: Int => Float32GeoTiffSegment =
    noDataValue match {
      case Some(nd) if isData(nd) =>
        { i: Int => new NoDataFloat32GeoTiffSegment(getDecompressedBytes(i), nd.toFloat) }
      case _ =>
        { i: Int => new Float32GeoTiffSegment(getDecompressedBytes(i)) }
    }
}
