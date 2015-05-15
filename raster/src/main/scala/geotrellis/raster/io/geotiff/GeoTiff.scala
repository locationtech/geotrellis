package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

trait GeoTiff {
  def imageData: GeoTiffImageData
  def extent: Extent
  def crs: CRS
  def tags: Tags

  def write(path: String): Unit =
    GeoTiffWriter.write(this, path)
}

object GeoTiff {
  def apply(tile: Tile, extent: Extent, crs: CRS): SingleBandGeoTiff =
    SingleBandGeoTiff(tile, extent, crs)

  def apply(tile: MultiBandTile, extent: Extent, crs: CRS): MultiBandGeoTiff =
    MultiBandGeoTiff(tile, extent, crs)
}
