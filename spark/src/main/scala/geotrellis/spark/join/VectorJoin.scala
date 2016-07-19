package geotrellis.spark.join

import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.tiling._

import com.vividsolutions.jts.geom.{Envelope => JtsEnvelope}
import com.vividsolutions.jts.index.strtree.STRtree
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import scala.collection.JavaConverters._
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

  def apply[
    L <% Geometry: ClassTag,
    R <% Geometry: ClassTag
  ](
    left: RDD[L],
    right: RDD[R],
    pred: (Geometry, Geometry) => Boolean
  )(implicit sc: SparkContext): RDD[(L, R)] = {

    // For simplicity, assume that the left RDD has length ≥ that
    // of the right one.
    val rtrees = right.mapPartitions({ partition =>
      val rtree = new STRtree

      partition.foreach({ r =>
        val Extent(xmin, ymin, xmax, ymax) = r.envelope
        val envelope = new JtsEnvelope(xmin, xmax, ymin, ymax)
        rtree.insert(envelope, r)
      })

      Iterator(rtree)
    }, preservesPartitioning = true)
      .zipWithIndex
      .map({ case (v, k) => (k, v) })
      .cache
    val count = rtrees.count.toInt

    // For every partition of the right-hand collection of items, find
    // an RDD of left-hand items that intersects with some member of
    // that partition.
    val rdds = (0 until count).map({ i =>
      val tree = sc.broadcast(rtrees.lookup(i).head)

      left.flatMap({ l =>
        val Extent(xmin, ymin, xmax, ymax) = l.envelope
        val envelope = new JtsEnvelope(xmin, xmax, ymin, ymax)

        tree.value.query(envelope)
          .asScala
          .map({ r: Any => r.asInstanceOf[R] })
          .filter({ r => pred(l, r) })
          .map({ r => (l, r) })
      })
    })

    // Return the results as a single RDD
    sc.union(rdds)
  }

}
