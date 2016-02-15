package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait UInt32GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UInt32GeoTiffSegment

  val bandType = UInt32BandType

  lazy val createSegment: Int => UInt32GeoTiffSegment =
    { i: Int => new UInt32GeoTiffSegment(getDecompressedBytes(i)) }
}
