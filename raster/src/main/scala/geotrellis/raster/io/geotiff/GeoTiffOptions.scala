package geotrellis.raster.io.geotiff

import geotrellis.raster.io.geotiff.compression._
import geotrellis.raster.io.geotiff.tags.codes.ColorSpace
import geotrellis.raster.render.IndexedColorMap

/**
  * This case class holds information about how the data is stored in
  * a [[GeoTiff]]. If no values are given directly, then the defaults
  * are used.
  */
case class GeoTiffOptions(
  storageMethod: StorageMethod = GeoTiffOptions.DEFAULT.storageMethod,
  compression: Compression = GeoTiffOptions.DEFAULT.compression,
  colorSpace: Int = GeoTiffOptions.DEFAULT.colorSpace,
  colorMap: Option[IndexedColorMap] = GeoTiffOptions.DEFAULT.colorMap
)

/**
 * The companion object to [[GeoTiffOptions]]
 */
object GeoTiffOptions {
  val DEFAULT = GeoTiffOptions(Striped, NoCompression, ColorSpace.BlackIsZero, None)

  /**
   * Creates a new instance of [[GeoTiffOptions]] with the given
   * StorageMethod and the default compression value
   */
  def apply(storageMethod: StorageMethod): GeoTiffOptions =
    DEFAULT.copy(storageMethod = storageMethod)

  /**
   * Creates a new instance of [[GeoTiffOptions]] with the given
   * Compression and the default [[StorageMethod]] value
   */
  def apply(compression: Compression): GeoTiffOptions =
    DEFAULT.copy(compression = compression)

  /**
   * Creates a new instance of [[GeoTiffOptions]] with the given color map.
   */
  def apply(colorMap: IndexedColorMap): GeoTiffOptions =
    DEFAULT.copy(colorSpace = ColorSpace.Palette, colorMap = Some(colorMap))
}
