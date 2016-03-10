package geotrellis.spark.mapalgebra.focal

import geotrellis.spark._

import reflect.ClassTag

object Implicits extends Implicits

trait Implicits  {
  implicit class withFocalRasterRDDMethods[K](val self: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: GridComponent[K])
      extends FocalRasterRDDMethods[K]
}
