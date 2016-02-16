package geotrellis.spark.elevation

import geotrellis.spark._

import reflect.ClassTag

object Implicits extends Implicits

trait Implicits  {
  implicit class withElevationRasterRDDMethods[K](val self: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends ElevationRasterRDDMethods[K] with Serializable
}
