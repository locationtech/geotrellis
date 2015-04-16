package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

@Lenses("_")
case class DataSampleFormatTags(
  sampleFormat: Array[Int] = Array(1),
  maxSampleValue: Option[Array[Long]] = None,
  minSampleValue: Option[Array[Long]] = None
)
