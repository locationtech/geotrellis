package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

case class SingleBandGeoTiff(tile: Tile, extent: Extent, crs: CRS, tags: Map[String, String], bandTags: Map[String, String]) {
  def projectedRaster: ProjectedRaster = ProjectedRaster(tile, extent, crs)
  def raster: Raster = Raster(tile, extent)
}

object SingleBandGeoTiff {
  def apply(path: String): SingleBandGeoTiff = 
    GeoTiffReader.readSingleBand(path)

  def apply(bytes: Array[Byte]): SingleBandGeoTiff = 
    GeoTiffReader.readSingleBand(bytes)

  def apply(path: String, decompress: Boolean): SingleBandGeoTiff = 
    GeoTiffReader.readSingleBand(path, decompress)

  def apply(bytes: Array[Byte], decompress: Boolean): SingleBandGeoTiff = 
    GeoTiffReader.readSingleBand(bytes, decompress)

  def compressed(path: String): SingleBandGeoTiff =
    GeoTiffReader.readSingleBand(path, false)

  def compressed(bytes: Array[Byte]): SingleBandGeoTiff =
    GeoTiffReader.readSingleBand(bytes, false)

  implicit def singleBandGeoTiffToTile(sbg: SingleBandGeoTiff): Tile =
    sbg.tile

  implicit def singleBandGeoTiffToRaster(sbg: SingleBandGeoTiff): Raster =
    sbg.raster

  implicit def singleBandGeoTiffToProjectedRaster(sbg: SingleBandGeoTiff): ProjectedRaster =
    sbg.projectedRaster
}
