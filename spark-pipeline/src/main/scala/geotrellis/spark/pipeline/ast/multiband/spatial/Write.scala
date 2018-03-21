package geotrellis.spark.pipeline.ast.multiband.spatial

import io.circe.syntax._

import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.write

import org.apache.spark.SparkContext

case class Write(
  node: Node[Stream[(Int, MultibandTileLayerRDD[SpatialKey])]],
  arg: write.JsonWrite
) extends Output[Stream[(Int, MultibandTileLayerRDD[SpatialKey])]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): Stream[(Int, MultibandTileLayerRDD[SpatialKey])] = Output.write(arg)(node.eval)
}
