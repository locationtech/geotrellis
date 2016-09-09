package geotrellis.spark.join

import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.tiling._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._

import scala.reflect._

object VectorJoin {
  /** Map each item to all the layout cells that it intersects with */
  def mapToKeys[T <% Geometry](rdd: RDD[T], layout: LayoutDefinition): RDD[(SpatialKey, T)] = {
    val mt = layout.mapTransform
    rdd.flatMap{ thing =>
      for { (col, row) <- mt((thing: Geometry).envelope).coords }
      yield SpatialKey(col, row) -> thing
    }
  }

  /**
    * Performs inner join between two RDDs of items that can be viewed as a Geometry.
    *
    * This transformation requires definition of a layout grid which will be used to join
    * all the geometries that overlap with corresponding cells. Choice of this layout grid may have
    * a large impact on performance. Each cell should cover an area equal to an area covered by average geometry.
    *
    * A choice that is too fine may result in unnecessarily large shuffle step, a choice that is too coarse will create
    * join partitions that may exceed the available memory of an executor.
    */
  def apply[
    L <% Geometry: ClassTag,
    R <% Geometry: ClassTag
  ](
    left: RDD[L],
    right: RDD[R],
    layout: LayoutDefinition,
    pred: (Geometry, Geometry) => Boolean
  ): RDD[(L, R)] = {
    mapToKeys(left, layout)
      .join(mapToKeys(right, layout))
      .values
      .filter { case (l, r) =>
        pred(l: Geometry, r: Geometry)
      }
  }
}
