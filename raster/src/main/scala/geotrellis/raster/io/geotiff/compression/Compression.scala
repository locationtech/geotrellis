package geotrellis.raster.io.geotiff.compression

trait Compression {
  def createCompressor(segmentCount: Int): Compressor
}
