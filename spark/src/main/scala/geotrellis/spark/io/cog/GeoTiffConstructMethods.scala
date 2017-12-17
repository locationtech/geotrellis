package geotrellis.spark.io.cog

import geotrellis.raster.{CellGrid, GridBounds}
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffOptions}
import geotrellis.spark.TileLayerMetadata
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util.MethodExtensions

trait GeoTiffConstructMethods[T <: CellGrid] extends MethodExtensions[T] {
  def toGeoTiff[K](
    nextLayout: LayoutDefinition,
    gb: GridBounds,
    md: TileLayerMetadata[K],
    options: GeoTiffOptions,
    overviews: List[GeoTiff[T]] = Nil
  ): GeoTiff[T]
}
