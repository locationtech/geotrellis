package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

@Lenses("_")
case class BasicTags(
  bitsPerSample: Int = 1, // This is written as an array per sample, but libtiff only takes one value, and so do we.
  colorMap: Seq[(Short, Short, Short)] = Seq(),
  imageLength: Int = 0,
  imageWidth: Int = 0,
  compression: Int = 1,
  photometricInterp: Int = -1,
  resolutionUnit: Option[Int] = None,
  rowsPerStrip: Long = 1,
  samplesPerPixel: Int = 1,
  stripByteCounts: Option[Array[Long]] = None,
  stripOffsets: Option[Array[Long]] = None,
  xResolution: Option[(Long, Long)] = None,
  yResolution: Option[(Long, Long)] = None
)
