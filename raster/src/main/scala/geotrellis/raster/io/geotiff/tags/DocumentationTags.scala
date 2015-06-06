package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

@Lenses("_")
case class DocumentationTags(
  documentName: Option[String] = None,
  pageName: Option[String] = None,
  pageNumber: Option[Array[Int]] = None,
  xPositions: Option[Array[(Long, Long)]] = None,
  yPositions: Option[Array[(Long, Long)]] = None
)
