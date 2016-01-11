package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Float32GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Float32GeoTiffSegment

  val bandType = Float32BandType

  val createSegment: Int => Float32GeoTiffSegment =
    { i: Int => new Float32GeoTiffSegment(getDecompressedBytes(i)) }
}
