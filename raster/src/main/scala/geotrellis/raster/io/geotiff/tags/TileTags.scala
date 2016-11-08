package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

@Lenses("_")
case class TileTags(
  tileWidth: Option[Long] = None,
  tileLength: Option[Long] = None,
  tileOffsets: Option[Array[Long]] = None,
  tileByteCounts: Option[Array[Long]] = None
)
