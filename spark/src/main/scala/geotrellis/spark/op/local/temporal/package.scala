package geotrellis.spark.op.local

import geotrellis.spark._

import reflect.ClassTag

package object temporal {

  implicit class LocalTemporalRasterRDDExtensions[K](val rasterRDD: RasterRDD[K])(
    implicit val keyClassTag: ClassTag[K],
    val _sc: SpatialComponent[K],
    val _tc: TemporalComponent[K]) extends LocalTemporalRasterRDDMethods[K] { }

  implicit class TemporalWindow[K](val rasterRDD: RasterRDD[K])(
    implicit val keyClassTag: ClassTag[K],
    _sc: SpatialComponent[K],
    _tc: TemporalComponent[K]) {

    import TemporalWindowHelper._

    def average: TemporalWindowState[K] = TemporalWindowState(rasterRDD, Average)

    def min: TemporalWindowState[K] = TemporalWindowState(rasterRDD, Minimum)

    def max: TemporalWindowState[K] = TemporalWindowState(rasterRDD, Maximum)

    def mode: TemporalWindowState[K] = TemporalWindowState(rasterRDD, Mode)

  }

}
