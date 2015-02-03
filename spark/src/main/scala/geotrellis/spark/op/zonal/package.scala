package geotrellis.spark.op

import geotrellis.spark._
import scala.reflect._

package object zonal {
  implicit class ZonalRasterRDDMethodExtensions[K](val rasterRDD: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K])
      extends ZonalRasterRDDMethods[K] with Serializable
}
