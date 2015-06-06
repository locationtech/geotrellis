package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

@Lenses("_")
case class ColimetryTags(
  whitePoints: Option[Array[(Long, Long)]] = None,
  primaryChromaticities: Option[Array[(Long, Long)]] = None,
  transferFunction: Option[Array[Int]] = None,
  transferRange: Option[Array[Int]] = None,
  referenceBlackWhite: Option[Array[Long]] = None
)
