package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline._
import geotrellis.spark.pipeline.json.Transform
import org.apache.spark.rdd.RDD

case class Group[T](
  tags: List[String],
  tag: String,
  `type`: String = "transform.group"
) extends Transform {
  def eval(labeledRDDs: List[(String, RDD[T])]): List[(String, RDD[T])] = {
    val (grdds, rdds) = labeledRDDs.partition { case (l, _) => tags.contains(l) }
    grdds.map { case (_, rdd) => tag -> rdd } ::: rdds
  }
}
