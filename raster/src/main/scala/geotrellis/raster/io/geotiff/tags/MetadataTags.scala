package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

import spire.syntax.cfor._

@Lenses("_")
case class MetadataTags(
  artist: Option[String] = None,
  copyright: Option[String] = None,
  dateTime: Option[String] = None,
  hostComputer: Option[String] = None,
  imageDesc: Option[String] = None,
  maker: Option[String] = None,
  model: Option[String] = None,
  software: Option[String] = None
)
