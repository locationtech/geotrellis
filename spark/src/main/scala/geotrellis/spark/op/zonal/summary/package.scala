package geotrellis.spark.op.zonal

import geotrellis.spark._
import scala.reflect._

package object summary {
  implicit class ZonalSummaryRasterRDDSourceExtensions[K](val rasterRDD: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends ZonalSummaryRasterRDDMethods[K] with Serializable
}
