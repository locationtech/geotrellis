package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

trait GeoTiffData {
  val cellType: CellType

  def imageData: GeoTiffImageData
  def extent: Extent
  def crs: CRS
  def tags: Tags
}

trait GeoTiff[T <: CellGrid] extends GeoTiffData {
  def tile: T

  def projectedRaster: ProjectedRaster[T] = ProjectedRaster(tile, extent, crs)
  def raster: Raster[T] = Raster(tile, extent)
  def rasterExtent: RasterExtent = RasterExtent(extent, tile.cols, tile.rows)

  def mapTile(f: T => T): GeoTiff[T]

  def write(path: String): Unit =
    GeoTiffWriter.write(this, path)

  def toByteArray: Array[Byte] =
    GeoTiffWriter.write(this)
}

object GeoTiff {
  def apply(tile: Tile, extent: Extent, crs: CRS): SingleBandGeoTiff =
    SingleBandGeoTiff(tile, extent, crs)

  def apply(raster: SingleBandRaster, crs: CRS): SingleBandGeoTiff =
    apply(raster.tile, raster.extent, crs)

  def apply(tile: MultiBandTile, extent: Extent, crs: CRS): MultiBandGeoTiff =
    MultiBandGeoTiff(tile, extent, crs)

  def apply(raster: MultiBandRaster, crs: CRS): MultiBandGeoTiff =
    apply(raster.tile, raster.extent, crs)
}
