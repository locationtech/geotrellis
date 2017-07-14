package geotrellis.spark.pipeline.ast.singleband.spatial

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.vector._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class BufferedReproject(
  node: Node[TileLayerRDD[SpatialKey]],
  arg: transform.BufferedReproject
) extends Transform[RDD[(ProjectedExtent, Tile)], (Int, TileLayerRDD[SpatialKey])] {
  def asJson = node.asJson :+ arg.asJson
  def get(implicit sc: SparkContext): (Int, TileLayerRDD[SpatialKey]) = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
