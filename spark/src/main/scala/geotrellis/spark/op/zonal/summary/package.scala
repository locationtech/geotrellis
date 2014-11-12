package geotrellis.spark.op.zonal

import geotrellis.spark._
import scala.reflect._

package object summary {
  implicit class ZonalSummaryRasterRDDSourceExtensions[K: SpatialComponent](
    val rasterRDD: RasterRDD[K])(implicit val keyClassTag: ClassTag[K])
      extends ZonalSummaryRasterRDDMethods[K] {
    protected val _sc = implicitly[SpatialComponent[K]]
  }
}
