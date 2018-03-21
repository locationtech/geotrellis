package geotrellis.spark.pipeline.ast.singleband.temporal

import io.circe.syntax._

import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.write

import org.apache.spark.SparkContext

case class Write(
  node: Node[Stream[(Int,TileLayerRDD[SpaceTimeKey])]],
  arg: write.JsonWrite
) extends Output[Stream[(Int, TileLayerRDD[SpaceTimeKey])]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): Stream[(Int, TileLayerRDD[SpaceTimeKey])] = Output.write(arg)(node.eval)
}
