package geotrellis.spark.op.local

import geotrellis.spark._

import reflect.ClassTag

package object temporal {

  implicit class LocalTemporalRasterRDDExtensions[K](val rasterRDD: RasterRDD[K])(
    implicit val keyClassTag: ClassTag[K],
    val _sc: SpatialComponent[K],
    val _tc: TemporalComponent[K]) extends LocalTemporalRasterRDDMethods[K] { }

}
