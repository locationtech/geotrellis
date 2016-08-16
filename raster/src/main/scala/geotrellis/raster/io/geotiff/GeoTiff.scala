package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

/**
 * Holds information on how the data is represented, projected, and any user
 * defined tags.
 */
trait GeoTiffData {
  val cellType: CellType

  def imageData: GeoTiffImageData
  def extent: Extent
  def crs: CRS
  def tags: Tags

  def pixelSampleType: Option[PixelSampleType] =
    tags.headTags.get(Tags.AREA_OR_POINT).flatMap { aop =>
      aop match {
        case "AREA" => Some(PixelIsArea)
        case "POINT" => Some(PixelIsPoint)
        case _ => None
      }
    }
}

/**
 * Base trait of GeoTiff. Takes a tile that is of a type equal to or a subtype
 * of CellGrid
 */
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

/**
 * Companion object to GeoTiff
 */
object GeoTiff {
  def apply(tile: Tile, extent: Extent, crs: CRS): SinglebandGeoTiff =
    SinglebandGeoTiff(tile, extent, crs)

  def apply(raster: SinglebandRaster, crs: CRS): SinglebandGeoTiff =
    apply(raster.tile, raster.extent, crs)

  def apply(tile: MultibandTile, extent: Extent, crs: CRS): MultibandGeoTiff =
    MultibandGeoTiff(tile, extent, crs)

  def apply(raster: MultibandRaster, crs: CRS): MultibandGeoTiff =
    apply(raster.tile, raster.extent, crs)
}
