package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

class MultiBandGeoTiff(
  val tile: MultiBandTile,
  val extent: Extent,
  val crs: CRS,
  val tags: Tags,
  options: GeoTiffOptions
) extends GeoTiff {
  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffMultiBandTile => gtt
      case _ => ??? ///tile.toGeoTiffTile(options) TODO: Implement this
    }
}

object MultiBandGeoTiff {
  def apply(path: String): MultiBandGeoTiff =
    GeoTiffReader.readMultiBand(path)

  /* Read a multi band GeoTIFF file.
   */
  def apply(path: String, decompress: Boolean): MultiBandGeoTiff = 
    GeoTiffReader.readMultiBand(path, decompress)

  /* Read a multi band GeoTIFF file.
   */
  def apply(bytes: Array[Byte]): MultiBandGeoTiff =
    GeoTiffReader.readMultiBand(bytes)

  def apply(bytes: Array[Byte], decompress: Boolean): MultiBandGeoTiff =
    GeoTiffReader.readMultiBand(bytes, decompress)

  def apply(
    tile: MultiBandTile,
    extent: Extent,
    crs: CRS
  ): MultiBandGeoTiff =
    apply(tile, extent, crs, Tags.empty)

  def apply(
    tile: MultiBandTile,
    extent: Extent,
    crs: CRS,
    tags: Tags
  ): MultiBandGeoTiff =
    apply(tile, extent, crs, tags, GeoTiffOptions.DEFAULT)

  def apply(
    tile: MultiBandTile,
    extent: Extent,
    crs: CRS,
    tags: Tags,
    options: GeoTiffOptions
  ): MultiBandGeoTiff =
    new MultiBandGeoTiff(tile, extent, crs, tags, options)

  def unapply(mbg: MultiBandGeoTiff): Option[(MultiBandTile, Extent, CRS, Tags)] =
    Some((mbg.tile, mbg.extent, mbg.crs, mbg.tags))

  implicit def multiBandGeoTiffToTile(mbg: MultiBandGeoTiff): MultiBandTile =
    mbg.tile
}
