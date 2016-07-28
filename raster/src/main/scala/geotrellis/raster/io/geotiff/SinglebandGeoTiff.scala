package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

case class SinglebandGeoTiff(
  val tile: Tile,
  val extent: Extent,
  val crs: CRS,
  val tags: Tags,
  val options: GeoTiffOptions
) extends GeoTiff[Tile] {
  val cellType = tile.cellType

  def mapTile(f: Tile => Tile): SinglebandGeoTiff =
    SinglebandGeoTiff(f(tile), extent, crs, tags, options)

  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffTile => gtt
      case _ => tile.toGeoTiffTile(options)
    }

  def crop(subExtent: Extent): SinglebandGeoTiff = {
    val raster: Raster[Tile] =
      this.crop(subExtent)

    SinglebandGeoTiff(raster, subExtent, this.crs)
  }
}

object SinglebandGeoTiff {

  def apply(
    tile: Tile,
    extent: Extent,
    crs: CRS
  ): SinglebandGeoTiff =
    new SinglebandGeoTiff(tile, extent, crs, Tags.empty, GeoTiffOptions.DEFAULT)

  /** Read a single-band GeoTIFF file from a byte array.
    * The GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(bytes: Array[Byte]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes)

  /** Read a single-band GeoTIFF file from a byte array.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(bytes: Array[Byte], decompress: Boolean, extent: Option[Extent], streaming: Boolean): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, decompress, extent, streaming)

  /** Read a single-band GeoTIFF file from the file at the given path.
    * The GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path)

  /** Read a single-band GeoTIFF file from the file at the given path.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String, decompress: Boolean, extent: Option[Extent], streaming: Boolean): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, decompress, extent, streaming)

  /** Read a single-band GeoTIFF file from the file at a given path.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, false, None, false)

  /** Read a single-band GeoTIFF file from a byte array.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(bytes: Array[Byte]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, false, None, false)

  def streaming(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, true, None, true)
  
  def streaming(bytes: Array[Byte]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, true, None, true)

  /** Read a windowed, singleband GeoTiff file from a given path.
   *  The file will be decompressed and read from [[BufferSegmentBytes]]
   */
  def windowed(path: String, extent: Extent): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, true, Some(extent), true)
  
  /** Read a windowed, singleband GeoTiff file from an Array[Byte].
   *  The file will be decompressed and read from [[BufferSegmentBytes]]
   */
  def windowed(bytes: Array[Byte], extent: Extent): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, true, Some(extent), true)

  implicit def singlebandGeoTiffToTile(sbg: SinglebandGeoTiff): Tile =
    sbg.tile

  implicit def singlebandGeoTiffToRaster(sbg: SinglebandGeoTiff): SinglebandRaster =
    sbg.raster

  implicit def singlebandGeoTiffToProjectedRaster(sbg: SinglebandGeoTiff): ProjectedRaster[Tile] =
    sbg.projectedRaster
}
