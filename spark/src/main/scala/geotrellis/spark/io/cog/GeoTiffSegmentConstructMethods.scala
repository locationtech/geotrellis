package geotrellis.spark.io.cog

import geotrellis.proj4.CRS
import geotrellis.raster.io.geotiff._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector.Extent
import geotrellis.util._

trait GeoTiffSegmentConstructMethods[K, T <: CellGrid] extends MethodExtensions[Iterable[(K, T)]] {
  def toGeoTiff(
    layout: LayoutDefinition,
    extent: Extent,
    crs: CRS,
    options: GeoTiffOptions,
    tags: Tags = Tags.empty,
    overviews: List[GeoTiff[T]] = Nil
  )(implicit sc: SpatialComponent[K]): GeoTiff[T]
}
