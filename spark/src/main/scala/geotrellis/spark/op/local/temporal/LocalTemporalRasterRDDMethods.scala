package geotrellis.spark.op.local.temporal

import geotrellis.spark._

trait LocalTemporalRasterRDDMethods[K] extends RasterRDDMethods[K] {

  import TemporalWindow._

  val _sc: SpatialComponent[K]

  val _tc: TemporalComponent[K]

  //def average(periodStep: Int, )

}
