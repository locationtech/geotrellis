package geotrellis.spark.pipeline.ast.multiband.spatial

import io.circe.syntax._

import geotrellis.spark.pipeline.ast._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.json.write

import org.apache.spark.SparkContext

case class HadoopWrite(
  node: Node[Stream[(Int, MultibandTileLayerRDD[SpatialKey])]],
  arg: write.Hadoop
) extends Write[Stream[(Int, MultibandTileLayerRDD[SpatialKey])]] {
  def asJson = node.asJson :+ arg.asJson
  def get(implicit sc: SparkContext): Stream[(Int, MultibandTileLayerRDD[SpatialKey])] = Write.eval(arg)(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}