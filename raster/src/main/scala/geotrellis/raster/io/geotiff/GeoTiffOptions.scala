package geotrellis.raster.io.geotiff

import geotrellis.raster.io.geotiff.compression._

/**
 * This case class holds information about how the data is stored in a
 * [[GeoTiff]]. If no values are given directly, then the defaults
 * are used.
 */
case class GeoTiffOptions(
  storageMethod: StorageMethod = GeoTiffOptions.DEFAULT.storageMethod,
  compression: Compression = GeoTiffOptions.DEFAULT.compression
)

/** The companion object to [[GeoTiffOptions]] */
object GeoTiffOptions {
  val DEFAULT = GeoTiffOptions(Striped, NoCompression)

  /** Creates a new instance of [[GeoTiffOptions]] with the given StorageMethod
   *  and the default [[Compression]] value
   */
  def apply(storageMethod: StorageMethod): GeoTiffOptions =
    GeoTiffOptions(storageMethod, DEFAULT.compression)

  /** Creates a new instance of [[GeoTiffOptions]] with the given Compression
   *  and the default [[StorageMethod]] value
   */
  def apply(compression: Compression): GeoTiffOptions =
    GeoTiffOptions(DEFAULT.storageMethod, compression)
}
