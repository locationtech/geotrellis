package geotrellis.raster.io.geotiff

import geotrellis.util.ByteReader
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
      this.raster.crop(subExtent)

    MultibandGeoTiff(raster, subExtent, this.crs, this.tags)
  }

  def crop(colMax: Int, rowMax: Int): MultibandGeoTiff =
    crop(0, 0, colMax, rowMax)

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): MultibandGeoTiff = {
    val raster: Raster[MultibandTile] =
      this.raster.crop(colMin, rowMin, colMax, rowMax)

    MultibandGeoTiff(raster, raster._2, this.crs, this.tags)
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
  def apply(bytes: Array[Byte], decompress: Boolean, streaming: Boolean): MultibandGeoTiff =
    GeoTiffReader.readMultiband(bytes, decompress, streaming)

  /** Read a multi-band GeoTIFF file from the file at the given path.
    * GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path)

  def apply(path: String, e: Extent): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, e)

  def apply(path: String, e: Option[Extent]): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, e)

  /** Read a multi-band GeoTIFF file from the file at the given path.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String, decompress: Boolean, streaming: Boolean): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, decompress, streaming)

  def apply(byteReader: ByteReader): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader)

  def apply(byteReader: ByteReader, e: Extent): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, e)

  def apply(byteReader: ByteReader, e: Option[Extent]): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, e)

  def apply(byteReader: ByteReader, decompress: Boolean, streaming: Boolean): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, decompress, streaming)

  /** Read a multi-band GeoTIFF file from the file at a given path.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(path: String): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, false, false)

  /** Read a multi-band GeoTIFF file from a byte array.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(bytes: Array[Byte]): MultibandGeoTiff =
    GeoTiffReader.readMultiband(bytes, false, false)

  def streaming(path: String): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, false, true)

  def streaming(byteReader: ByteReader): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, false, true)

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
}
