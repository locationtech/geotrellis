package geotrellis.spark.split

import geotrellis.raster._
import geotrellis.raster.split.SplitMethods
import geotrellis.spark._
import geotrellis.vector.ProjectedExtent
import geotrellis.util._

import org.apache.spark.rdd.RDD

object Implicits extends Implicits

trait Implicits {
  implicit class withProjectedExtentRDDSplitMethods[
    K: Component[?, ProjectedExtent],
    V <: CellGrid: (? => SplitMethods[V])
  ](val self: RDD[(K, V)]) extends ProjectedExtentRDDSplitMethods[K, V]
}
