package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

trait GeoTiff {
  def imageData: GeoTiffImageData
  def extent: Extent
  def crs: CRS
  def tags: Tags
}

object GeoTiff {
  def apply(tile: Tile, extent: Extent, crs: CRS): SingleBandGeoTiff =
    SingleBandGeoTiff(tile, extent, crs)
}
