package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Int32GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Int32GeoTiffSegment

  val bandType = Int32BandType

  val createSegment: Int => Int32GeoTiffSegment =
    { i: Int => new Int32GeoTiffSegment(getDecompressedBytes(i)) }
}
