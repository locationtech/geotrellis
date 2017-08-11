package geotrellis.spark.pipeline.ast.singleband.temporal

import io.circe.syntax._

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.write

import org.apache.spark.SparkContext

case class HadoopWrite(
  node: Node[Stream[(Int, TileLayerRDD[SpaceTimeKey])]],
  arg: write.JsonWrite
) extends Write[Stream[(Int, TileLayerRDD[SpaceTimeKey])]] {
  def asJson = node.asJson :+ arg.asJson
  def get(implicit sc: SparkContext): Stream[(Int, TileLayerRDD[SpaceTimeKey])] = Write.eval(arg)(node.get)
}