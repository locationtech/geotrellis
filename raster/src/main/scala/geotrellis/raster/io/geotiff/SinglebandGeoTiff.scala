package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffStreamReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

import java.nio.ByteBuffer

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
  
  def apply(bytes: ByteBuffer): SinglebandGeoTiff =
    GeoTiffStreamReader.readSingleband(bytes)

  /** Read a single-band GeoTIFF file from a byte array.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(bytes: Array[Byte], decompress: Boolean): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, decompress)

  def apply(bytes: ByteBuffer, decompress: Boolean): SinglebandGeoTiff =
    GeoTiffStreamReader.readSingleband(bytes, decompress)

  /** Read a single-band GeoTIFF file from the file at the given path.
    * The GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String): SinglebandGeoTiff =
    GeoTiffStreamReader.readSingleband(path)

  /** Read a single-band GeoTIFF file from the file at the given path.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String, decompress: Boolean): SinglebandGeoTiff =
    GeoTiffStreamReader.readSingleband(path, decompress)

  /** Read a single-band GeoTIFF file from the file at a given path.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, false)

  def streamCompressed(path: String): SinglebandGeoTiff =
    GeoTiffStreamReader.readSingleband(path, false)

  /** Read a single-band GeoTIFF file from a byte array.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(bytes: Array[Byte]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, false)

  def streamCompressed(bytes: ByteBuffer): SinglebandGeoTiff =
    GeoTiffStreamReader.readSingleband(bytes, false)

  implicit def singlebandGeoTiffToTile(sbg: SinglebandGeoTiff): Tile =
    sbg.tile

  implicit def singlebandGeoTiffToRaster(sbg: SinglebandGeoTiff): SinglebandRaster =
    sbg.raster

  implicit def singlebandGeoTiffToProjectedRaster(sbg: SinglebandGeoTiff): ProjectedRaster[Tile] =
    sbg.projectedRaster
}
