package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

@Lenses("_")
case class GeoTiffTags(
  modelTiePoints: Option[Array[(Pixel3D, Pixel3D)]] = None,
  modelTransformation: Option[Array[Array[Double]]] = None,
  modelPixelScale: Option[(Double, Double, Double)] = None,
  geoKeyDirectory: Option[GeoKeyDirectory] = None,
  doubles: Option[Array[Double]] = None,
  asciis: Option[String] = None,
  metadata: Option[String] = None,
  gdalInternalNoData: Option[Double] = None
)
