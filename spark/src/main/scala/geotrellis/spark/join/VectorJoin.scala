package geotrellis.spark.join

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util.annotations.experimental
import geotrellis.vector._

import com.vividsolutions.jts.geom.{ Envelope => JtsEnvelope }
import com.vividsolutions.jts.index.strtree.STRtree
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import scala.collection.JavaConverters._
import scala.reflect._

@experimental
object VectorJoin {
  @experimental
  def apply[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ](
    longer: RDD[L],
    shorter: RDD[R],
    pred: (Geometry, Geometry) => Boolean
  )(implicit sc: SparkContext): RDD[(L, R)] = {
    val rtrees = shorter.mapPartitions({ partition =>
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

    // For every partition of the right-hand (smaller) collection of
    // items, find an RDD of items from the longer collection that
    // intersects with some member of that partition partition.
    val rdds = (0 until count).map({ i =>
      val tree = sc.broadcast(rtrees.lookup(i).head)

      longer.flatMap({ l =>
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
