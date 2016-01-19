package geotrellis.spark.op.local.spatial

import geotrellis.raster._
import geotrellis.spark._
import org.apache.spark.rdd.RDD
import reflect.ClassTag

object Implicits extends Implicits

trait Implicits  {
  implicit class withLocalSpatialRasterRDDMethods[K: SpatialComponent](val self: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K]) extends LocalSpatialRasterRDDMethods[K]
}
