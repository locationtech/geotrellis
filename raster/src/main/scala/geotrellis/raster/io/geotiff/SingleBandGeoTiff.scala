package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

case class SingleBandGeoTiff(
  val tile: Tile,
  val extent: Extent,
  val crs: CRS,
  val tags: Tags,
  val options: GeoTiffOptions
) extends GeoTiff[Tile] {
  val cellType = tile.cellType

  def mapTile(f: Tile => Tile): SingleBandGeoTiff =
    SingleBandGeoTiff(f(tile), extent, crs, tags, options)

  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffTile => gtt
      case _ => tile.toGeoTiffTile(options)
    }
}

object SingleBandGeoTiff {

  def apply(
    tile: Tile,
    extent: Extent,
    crs: CRS
  ): SingleBandGeoTiff =
    new SingleBandGeoTiff(tile, extent, crs, Tags.empty, GeoTiffOptions.DEFAULT)

  /** Read a single-band GeoTIFF file from a byte array.
    * The GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(bytes: Array[Byte]): SingleBandGeoTiff =
    GeoTiffReader.readSingleBand(bytes)

  /** Read a single-band GeoTIFF file from a byte array.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(bytes: Array[Byte], decompress: Boolean): SingleBandGeoTiff =
    GeoTiffReader.readSingleBand(bytes, decompress)

  /** Read a single-band GeoTIFF file from the file at the given path.
    * The GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String): SingleBandGeoTiff =
    GeoTiffReader.readSingleBand(path)

  /** Read a single-band GeoTIFF file from the file at the given path.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String, decompress: Boolean): SingleBandGeoTiff =
    GeoTiffReader.readSingleBand(path, decompress)


  /** Read a single-band GeoTIFF file from the file at a given path.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(path: String): SingleBandGeoTiff =
    GeoTiffReader.readSingleBand(path, false)

  /** Read a single-band GeoTIFF file from a byte array.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(bytes: Array[Byte]): SingleBandGeoTiff =
    GeoTiffReader.readSingleBand(bytes, false)

  implicit def singleBandGeoTiffToTile(sbg: SingleBandGeoTiff): Tile =
    sbg.tile

  implicit def singleBandGeoTiffToRaster(sbg: SingleBandGeoTiff): SingleBandRaster =
    sbg.raster

  implicit def singleBandGeoTiffToProjectedRaster(sbg: SingleBandGeoTiff): ProjectedRaster[Tile] =
    sbg.projectedRaster
}
