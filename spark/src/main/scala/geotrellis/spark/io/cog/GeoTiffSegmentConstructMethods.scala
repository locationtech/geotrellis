package geotrellis.spark.io.cog

import geotrellis.raster.io.geotiff._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._

trait GeoTiffSegmentConstructMethods[K, T <: CellGrid] extends MethodExtensions[Iterable[(K, T)]] {
  implicit val spatialComponent: SpatialComponent[K]
  lazy val gridBounds: GridBounds = GridBounds.envelope(self.map(_._1.getComponent[SpatialKey]))

  def toGeoTiff(
    nextLayout: LayoutDefinition,
    md: TileLayerMetadata[K],
    options: GeoTiffOptions,
    overviews: List[GeoTiff[T]] = Nil,
    print: Boolean = false
  ): GeoTiff[T]
}
