package geotrellis.spark.pipeline.ast.singleband.spatial

import io.circe.syntax._

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.write

import org.apache.spark.SparkContext

case class Write(
  node: Node[Stream[(Int, TileLayerRDD[SpatialKey])]],
  arg: write.JsonWrite
) extends Output[Stream[(Int, TileLayerRDD[SpatialKey])]] {
  def asJson = node.asJson :+ arg.asJson
  def get(implicit sc: SparkContext): Stream[(Int, TileLayerRDD[SpatialKey])] = Output.eval(arg)(node.get)
}