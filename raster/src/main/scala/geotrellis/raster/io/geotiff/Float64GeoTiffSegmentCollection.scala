package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Float64GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Float64GeoTiffSegment

  val bandType = Float64BandType

  val createSegment: Int => Float64GeoTiffSegment =
    { i: Int => new Float64GeoTiffSegment(getDecompressedBytes(i)) }
}
