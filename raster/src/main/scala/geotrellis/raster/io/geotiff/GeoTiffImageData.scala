package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait GeoTiffImageData {
  def cols: Int
  def rows: Int
  def bandType: BandType
  def bandCount: Int
  def compressedBytes: Array[Array[Byte]]
  def decompressor: Decompressor
  def segmentLayout: GeoTiffSegmentLayout
}
