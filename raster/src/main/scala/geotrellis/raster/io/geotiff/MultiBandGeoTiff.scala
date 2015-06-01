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
  def mapTile(f: MultiBandTile => MultiBandTile): MultiBandGeoTiff =
    MultiBandGeoTiff(f(tile), extent, crs, tags, options)

  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffMultiBandTile => gtt
      case _ => GeoTiffMultiBandTile(tile)
    }
}

object MultiBandGeoTiff {
  /** Read a multi-band GeoTIFF file from a byte array.
    * GeoTIFF will be fully uncompressed and held in memory.
    */
  def apply(bytes: Array[Byte]): MultiBandGeoTiff = 
    GeoTiffReader.readMultiBand(bytes)

  /** Read a multi-band GeoTIFF file from a byte array.
    * If decompress = true, the GeoTIFF will be fully uncompressed and held in memory.
    */
  def apply(bytes: Array[Byte], decompress: Boolean): MultiBandGeoTiff = 
    GeoTiffReader.readMultiBand(bytes, decompress)

  /** Read a multi-band GeoTIFF file from the file at the given path.
    * GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String): MultiBandGeoTiff = 
    GeoTiffReader.readMultiBand(path)

  /** Read a multi-band GeoTIFF file from the file at the given path.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String, decompress: Boolean): MultiBandGeoTiff = 
    GeoTiffReader.readMultiBand(path, decompress)


  /** Read a multi-band GeoTIFF file from the file at a given path.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(path: String): MultiBandGeoTiff =
    GeoTiffReader.readMultiBand(path, false)


  /** Read a multi-band GeoTIFF file from a byte array.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(bytes: Array[Byte]): MultiBandGeoTiff =
    GeoTiffReader.readMultiBand(bytes, false)

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
