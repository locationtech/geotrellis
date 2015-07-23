package geotrellis.spark.op

import geotrellis.spark.RasterRDD
import scala.reflect.ClassTag
import geotrellis.raster.Tile

package object stats {
  implicit class StatsRasterRDDSourceExtensions[K](val rasterRDD: RasterRDD[K, Tile])(implicit val keyClassTag: ClassTag[K])
    extends StatsRasterRDDMethods[K] { }
}
