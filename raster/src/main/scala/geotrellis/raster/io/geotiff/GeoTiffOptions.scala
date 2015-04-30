package geotrellis.raster.io.geotiff

import geotrellis.raster.io.geotiff.compression._

case class GeoTiffOptions(
  storageMethod: StorageMethod = GeoTiffOptions.DEFAULT.storageMethod,
  compression: Compression = GeoTiffOptions.DEFAULT.compression
)

object GeoTiffOptions {
  val DEFAULT = GeoTiffOptions(Striped, NoCompression)

  def apply(storageMethod: StorageMethod): GeoTiffOptions =
    GeoTiffOptions(storageMethod, DEFAULT.compression)

  def apply(compression: Compression): GeoTiffOptions =
    GeoTiffOptions(DEFAULT.storageMethod, compression)
}
