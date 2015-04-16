package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

@Lenses("_")
case class NonBasicTags(
  cellLength: Option[Int] = None,
  cellWidth: Option[Int] = None,
  extraSamples: Option[Array[Int]] = None,
  fillOrder: Int = 1,
  freeByteCounts: Option[Array[Long]] = None,
  freeOffsets: Option[Array[Long]] = None,
  grayResponseCurve: Option[Array[Int]] = None,
  grayResponseUnit: Option[Int] = None,
  newSubfileType: Option[Long] = None,
  orientation: Int = 1,
  planarConfiguration: Option[Int] = None,
  subfileType: Option[Int] = None,
  thresholding: Int = 1,
  t4Options: Int = 0,
  t6Options: Option[Int] = None,
  halftoneHints: Option[Array[Int]] = None,
  predictor: Option[Int] = None
)
