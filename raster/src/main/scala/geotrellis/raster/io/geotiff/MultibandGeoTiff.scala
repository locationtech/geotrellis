package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

case class MultibandGeoTiff(
  val tile: MultibandTile,
  val extent: Extent,
  val crs: CRS,
  val tags: Tags,
  options: GeoTiffOptions
) extends GeoTiff[MultibandTile] {
  val cellType = tile.cellType

  def mapTile(f: MultibandTile => MultibandTile): MultibandGeoTiff =
    MultibandGeoTiff(f(tile), extent, crs, tags, options)

  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffMultibandTile => gtt
      case _ => GeoTiffMultibandTile(tile)
    }
  
  def crop(subExtent: Extent): MultibandGeoTiff = {
    val raster: Raster[MultibandTile] =
      (this.crop(subExtent)).raster

    MultibandGeoTiff(raster, subExtent, this.crs, this.tags)
  }
}

object MultibandGeoTiff {
  /** Read a multi-band GeoTIFF file from a byte array.
    * GeoTIFF will be fully uncompressed and held in memory.
    */
  def apply(bytes: Array[Byte]): MultibandGeoTiff =
    GeoTiffReader.readMultiband(bytes)

  /** Read a multi-band GeoTIFF file from a byte array.
    * If decompress = true, the GeoTIFF will be fully uncompressed and held in memory.
    */
  def apply(bytes: Array[Byte], decompress: Boolean, extent: Option[Extent], streaming: Boolean): MultibandGeoTiff =
    GeoTiffReader.readMultiband(bytes, decompress, extent, streaming)

  /** Read a multi-band GeoTIFF file from the file at the given path.
    * GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path)

  /** Read a multi-band GeoTIFF file from the file at the given path.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String, decompress: Boolean, extent: Option[Extent], streaming: Boolean): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, decompress, extent, streaming)

  /** Read a multi-band GeoTIFF file from the file at a given path.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(path: String): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, false, None, false)

  /** Read a multi-band GeoTIFF file from a byte array.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(bytes: Array[Byte]): MultibandGeoTiff =
    GeoTiffReader.readMultiband(bytes, false, None, false)
  
  def streaming(path: String): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, true, None, true)
  
  def streaming(bytes: Array[Byte]): MultibandGeoTiff =
    GeoTiffReader.readMultiband(bytes, true, None, true)

  /** Read a windowed, multiband GeoTiff file from a given path.
    * The file will be decompressed and read from [[BufferSegmentBytes]]
    */
  def windowed(path: String, extent: Extent): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, true, Some(extent), true)
  
  /** Read a windowed, multiband GeoTiff file from an Array[Byte].
    * The file will be decompressed and read from [[BufferSegmentBytes]]
    */
  def windowed(bytes: Array[Byte], extent: Extent): MultibandGeoTiff =
    GeoTiffReader.readMultiband(bytes, true, Some(extent), true)

  def apply(
    tile: MultibandTile,
    extent: Extent,
    crs: CRS
  ): MultibandGeoTiff =
    apply(tile, extent, crs, Tags.empty)

  def apply(
    tile: MultibandTile,
    extent: Extent,
    crs: CRS,
    tags: Tags
  ): MultibandGeoTiff =
    apply(tile, extent, crs, tags, GeoTiffOptions.DEFAULT)

  implicit def multibandGeoTiffToTile(mbg: MultibandGeoTiff): MultibandTile =
    mbg.tile
}
