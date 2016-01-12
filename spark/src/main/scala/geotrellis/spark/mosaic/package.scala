package geotrellis.spark

import geotrellis.raster._
import geotrellis.raster.mosaic._
import geotrellis.raster.prototype._
import geotrellis.spark.tiling.LayoutDefinition
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

package object mosaic {
  implicit class withRddMergeMethods[K: ClassTag, V <: CellGrid: MergeView: ClassTag](rdd: RDD[(K, V)])
    extends RddMergeMethods[K, V](rdd)

  implicit class withRddLayoutMergeMethods[
    K: SpatialComponent: ClassTag, 
    V <: CellGrid: MergeView: ClassTag: (? => TilePrototypeMethods[V]),
    M: (? => {def layout: LayoutDefinition})
  ](rdd: RDD[(K, V)] with Metadata[M]) extends RddLayoutMergeMethods[K, V, M](rdd)

}
