package geotrellis.raster.io.geotiff

import geotrellis.raster.io.geotiff.compression._
import geotrellis.raster.io.geotiff.tags.codes.ColorSpace

/**
  * This case class holds information about how the data is stored in
  * a [[GeoTiff]]. If no values are given directly, then the defaults
  * are used.
  */
case class GeoTiffOptions(
  storageMethod: StorageMethod = GeoTiffOptions.DEFAULT.storageMethod,
  compression: Compression = GeoTiffOptions.DEFAULT.compression,
  colorSpace: Int = GeoTiffOptions.DEFAULT.colorSpace
)

/**
  * The companion object to [[GeoTiffOptions]]
  */
object GeoTiffOptions {
  val DEFAULT = GeoTiffOptions(Striped, NoCompression, ColorSpace.BlackIsZero)

  /**
    * Creates a new instance of [[GeoTiffOptions]] with the given
    * StorageMethod and the default compression value
    */
  def apply(storageMethod: StorageMethod): GeoTiffOptions =
    GeoTiffOptions(storageMethod, DEFAULT.compression, DEFAULT.colorSpace)

  /**
    * Creates a new instance of [[GeoTiffOptions]] with the given
    * Compression and the default [[StorageMethod]] value
    */
  def apply(compression: Compression): GeoTiffOptions =
    GeoTiffOptions(DEFAULT.storageMethod, compression, DEFAULT.colorSpace)
}
