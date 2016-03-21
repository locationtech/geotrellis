package geotrellis.spark.mapalgebra.local.temporal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits  {

  implicit class withLocalTemporalTileRDDMethods[K: ClassTag: SpatialComponent: TemporalComponent](self: RDD[(K, Tile)])
      extends LocalTemporalTileRDDMethods[K](self)

  implicit class TemporalWindow[K: ClassTag: SpatialComponent: TemporalComponent](val self: RDD[(K, Tile)]) extends MethodExtensions[RDD[(K, Tile)]] {

    import TemporalWindowHelper._

    def average: TemporalWindowState[K] = TemporalWindowState(self, Average)

    def minimum: TemporalWindowState[K] = TemporalWindowState(self, Minimum)

    def maximum: TemporalWindowState[K] = TemporalWindowState(self, Maximum)

    def variance: TemporalWindowState[K] = TemporalWindowState(self, Variance)
  }
}
