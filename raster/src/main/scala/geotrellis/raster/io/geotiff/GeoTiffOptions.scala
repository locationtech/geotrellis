package geotrellis.raster.io.geotiff

import geotrellis.raster.io.geotiff.compression._

case class GeoTiffOptions(
  layout: GeoTiffLayout = GeoTiffOptions.DEFAULT.layout,
  compression: Compression = GeoTiffOptions.DEFAULT.compression
)

object GeoTiffOptions {
  val DEFAULT = GeoTiffOptions(Striped, NoCompression)

  def apply(layout: GeoTiffLayout): GeoTiffOptions =
    GeoTiffOptions(layout, DEFAULT.compression)

  def apply(compression: Compression): GeoTiffOptions =
    GeoTiffOptions(DEFAULT.layout, compression)
}
