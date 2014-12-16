package geotrellis.spark

import reflect.ClassTag

package object op {

  implicit class TemporalWindow[K](val rasterRDD: RasterRDD[K])(
    implicit val keyClassTag: ClassTag[K],
    _sc: SpatialComponent[K],
    _tc: TemporalComponent[K]) {

    import TemporalWindowState._

    def average: TemporalWindowState[K] = TemporalWindowState(rasterRDD, Average)

    def min: TemporalWindowState[K] = TemporalWindowState(rasterRDD, Minimum)

    def max: TemporalWindowState[K] = TemporalWindowState(rasterRDD, Maximum)

    def mode: TemporalWindowState[K] = TemporalWindowState(rasterRDD, Mode)

  }

}
