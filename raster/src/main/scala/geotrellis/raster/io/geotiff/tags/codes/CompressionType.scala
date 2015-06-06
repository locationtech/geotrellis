package geotrellis.raster.io.geotiff.tags.codes

object CompressionType {

  val Uncompressed = 1
  val HuffmanCoded = 2
  val GroupThreeCoded = 3
  val GroupFourCoded = 4
  val LZWCoded = 5
  val JpegOldCoded = 6
  val JpegCoded = 7
  val ZLibCoded = 8
  val PackBitsCoded = 32773
  val PkZipCoded = 32946

}
