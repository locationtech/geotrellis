package geotrellis.spark.op.zonal

import geotrellis.spark._
import scala.reflect._
import geotrellis.raster.Tile

package object summary {
  implicit class ZonalSummaryRasterRDDMethodExtensions[K](val rasterRDD: RasterRDD[K, Tile])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends ZonalSummaryRasterRDDMethods[K] with Serializable
}
