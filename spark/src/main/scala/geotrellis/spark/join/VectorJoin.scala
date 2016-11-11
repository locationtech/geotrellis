package geotrellis.spark.join

import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.tiling._

import com.vividsolutions.jts.geom.{ Envelope => JtsEnvelope }
import com.vividsolutions.jts.index.strtree.STRtree
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import scala.collection.JavaConverters._
import scala.reflect._


object VectorJoin {

  def apply[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ](
    shorter: RDD[L],
    longer: RDD[R],
    pred: (Geometry, Geometry) => Boolean
  )(implicit sc: SparkContext): RDD[(L, R)] = {
    val rtrees = longer.mapPartitions({ partition =>
      val rtree = new STRtree

      partition.foreach({ r =>
        val Extent(xmin, ymin, xmax, ymax) = r.envelope
        val envelope = new JtsEnvelope(xmin, xmax, ymin, ymax)
        rtree.insert(envelope, r)
      })

      Iterator(rtree)
    }, preservesPartitioning = true)
      .cache

    shorter.cartesian(rtrees).flatMap({ case (left, tree) =>
      val Extent(xmin, ymin, xmax, ymax) = left.envelope
      val envelope = new JtsEnvelope(xmin, xmax, ymin, ymax)

      tree.query(envelope)
        .asScala
        // .map({ right: Any => right.asInstanceOf[R] })
        .filter({ right: Any => pred(left, right.asInstanceOf[R]) })
        .map({ right: Any => (left, right.asInstanceOf[R]) })
    })
  }
}
