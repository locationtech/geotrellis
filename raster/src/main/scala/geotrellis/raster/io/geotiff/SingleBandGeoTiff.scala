package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

case class SingleBandGeoTiff(tile: Tile, extent: Extent, crs: CRS, tags: Map[String, String]) {
  def raster: Raster = Raster(tile, extent)
}

object SingleBandGeoTiff {
  def apply(path: String): SingleBandGeoTiff = 
    GeoTiffReader.readSingleBand(path)

  def readSingleBand(bytes: Array[Byte]): SingleBandGeoTiff = 
    GeoTiffReader.readSingleBand(bytes)
}
