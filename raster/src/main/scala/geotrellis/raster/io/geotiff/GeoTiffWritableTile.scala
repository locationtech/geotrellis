package geotrellis.raster.io.geotiff

import geotrellis.raster.io.geotiff.compression.Decompressor

trait GeoTiffWritableTile {
  def bandType: BandType
  def compressedBytes: Array[Array[Byte]]
  def decompressor: Decompressor
  def segmentLayout: GeoTiffSegmentLayout
}
