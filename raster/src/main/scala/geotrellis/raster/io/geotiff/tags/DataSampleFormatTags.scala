package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

@Lenses("_")
case class DataSampleFormatTags(
  sampleFormat: Int = 1, // Written as an array per sample, but libtiff only reads as one Int, and so do we.
  maxSampleValue: Option[Array[Long]] = None,
  minSampleValue: Option[Array[Long]] = None
)
