package geotrellis.spark.op

import geotrellis.spark.RasterRDD
import scala.reflect.ClassTag

package object stats {
  implicit class StatsRasterRDDSourceExtensions[K](val rasterRDD: RasterRDD[K])(implicit val keyClassTag: ClassTag[K])
    extends StatsRasterRDDMethods[K] { }
}
