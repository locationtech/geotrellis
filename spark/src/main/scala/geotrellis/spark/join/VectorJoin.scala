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

  def unflattened[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ](
    shorter: RDD[L],
    longer: RDD[R],
    pred: (Geometry, Geometry) => Boolean
  )(implicit sc: SparkContext): RDD[(L, Seq[R])] = {
    val rtrees = longer.mapPartitions({ partition =>
      val rtree = new STRtree

      partition.foreach({ r =>
        val Extent(xmin, ymin, xmax, ymax) = r.envelope
        val envelope = new JtsEnvelope(xmin, xmax, ymin, ymax)
        rtree.insert(envelope, r)
      })

      Iterator(rtree)
    }, preservesPartitioning = true)

    shorter.cartesian(rtrees).map({ case (left, tree) =>
      val Extent(xmin, ymin, xmax, ymax) = left.envelope
      val envelope = new JtsEnvelope(xmin, xmax, ymin, ymax)
      val rights = tree.query(envelope)
        .asScala
        .map({ right: Any => right.asInstanceOf[R] })
        .filter({ right => pred(left, right) })

      (left, rights)
    })
  }

  def apply[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ](
    shorter: RDD[L],
    longer: RDD[R],
    pred: (Geometry, Geometry) => Boolean
  )(implicit sc: SparkContext): RDD[(L, R)] =
    unflattened(shorter, longer, pred)
      .flatMap({ case (left, rights) => rights.map({ right => (left, right) }) })

}
