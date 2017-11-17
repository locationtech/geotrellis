package geotrellis.spark.cog

import geotrellis.raster.io.geotiff._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util.MethodExtensions

trait GeoTiffSegmentConstructMethods[T <: CellGrid] extends MethodExtensions[Iterable[(SpatialKey, T)]] {
  lazy val gb: GridBounds = GridBounds.envelope(self.map(_._1))

  def toGeoTiff[K](
    nextLayout: LayoutDefinition,
    md: TileLayerMetadata[K],
    options: GeoTiffOptions,
    overviews: List[GeoTiff[T]] = Nil
  ): GeoTiff[T]
}
