package geotrellis.spark.mapalgebra.local.temporal

import geotrellis.raster._
import geotrellis.spark._
import org.apache.spark.rdd.RDD
import reflect.ClassTag

object Implicits extends Implicits

trait Implicits  {

  implicit class withLocalTemporalTileRDDMethods[K](val self: RDD[(K, Tile)])(
    implicit val keyClassTag: ClassTag[K],
    implicit val _sc: SpatialComponent[K],
    implicit val _tc: TemporalComponent[K]) extends LocalTemporalTileRDDMethods[K] { }

  implicit class TemporalWindow[K](val self: RDD[(K, Tile)])(
    implicit val keyClassTag: ClassTag[K],
    _sc: SpatialComponent[K],
    _tc: TemporalComponent[K]) {

    import TemporalWindowHelper._

    def average: TemporalWindowState[K] = TemporalWindowState(self, Average)

    def minimum: TemporalWindowState[K] = TemporalWindowState(self, Minimum)

    def maximum: TemporalWindowState[K] = TemporalWindowState(self, Maximum)

    def variance: TemporalWindowState[K] = TemporalWindowState(self, Variance)
  }
}
