package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

// TODO: Fix tags.
class MultiBandGeoTiff(
  val tile: MultiBandTile,
  val extent: Extent,
  val crs: CRS,
  val tags: Map[String, String],
  val bandTags: Map[String, String],
  options: GeoTiffOptions
) extends GeoTiff {
  def writable =
    tile match {
      case gtt: MultiBandGeoTiffTile => gtt
      case _ => ??? ///tile.toGeoTiffTile(options)
    }
}

// object MultiBandBandGeoTiff {

//   def unapply(mbg: MultiBandGeoTiff): Option[(Tile, Extent, CRS, Map[String, String], Map[String, String])] =
//     Some((mbg.tile, mbg.extent, mbg.crs, mbg.tags, mbg.bandTags))

//   def apply(
//     tile: Tile,
//     extent: Extent,
//     crs: CRS,
//     tags: Map[String, String],
//     bandTags: Map[String, String],
//     options: GeoTiffOptions
//   ): SingleBandGeoTiff =
//     new SingleBandGeoTiff(tile, extent, crs, tags, bandTags, options)
  
//   /** Read a single-band GeoTIFF file from the file at the given path.
//     * The GeoTIFF will be fully uncompressed and held in memory.
//     */
//   def apply(path: String): SingleBandGeoTiff = 
//     GeoTiffReader.readSingleBand(path)

//   /** Read a single-band GeoTIFF file from a byte array.
//     * The GeoTIFF will be fully uncompressed and held in memory.
//     */
//   def apply(bytes: Array[Byte]): SingleBandGeoTiff = 
//     GeoTiffReader.readSingleBand(bytes)

//   /** Read a single-band GeoTIFF file from the file at the given path.
//     * If decompress = true, the GeoTIFF will be fully uncompressed and held in memory.
//     */
//   def apply(path: String, decompress: Boolean): SingleBandGeoTiff = 
//     GeoTiffReader.readSingleBand(path, decompress)

//   /** Read a single-band GeoTIFF file from a byte array.
//     * If decompress = true, the GeoTIFF will be fully uncompressed and held in memory.
//     */
//   def apply(bytes: Array[Byte], decompress: Boolean): SingleBandGeoTiff = 
//     GeoTiffReader.readSingleBand(bytes, decompress)

//   /** Read a single-band GeoTIFF file from the file at a given path.
//     * The tile data will remain tiled/striped and compressed in the TIFF format.
//     */
//   def compressed(path: String): SingleBandGeoTiff =
//     GeoTiffReader.readSingleBand(path, false)

//   /** Read a single-band GeoTIFF file from a byte array.
//     * The tile data will remain tiled/striped and compressed in the TIFF format.
//     */
//   def compressed(bytes: Array[Byte]): SingleBandGeoTiff =
//     GeoTiffReader.readSingleBand(bytes, false)

//   implicit def singleBandGeoTiffToTile(mbg: SingleBandGeoTiff): Tile =
//     mbg.tile

//   implicit def singleBandGeoTiffToRaster(mbg: SingleBandGeoTiff): Raster =
//     mbg.raster

//   implicit def singleBandGeoTiffToProjectedRaster(mbg: SingleBandGeoTiff): ProjectedRaster =
//     mbg.projectedRaster
// }
