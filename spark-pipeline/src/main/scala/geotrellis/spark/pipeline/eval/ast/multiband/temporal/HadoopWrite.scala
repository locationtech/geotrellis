package geotrellis.spark.pipeline.eval.ast.multiband.temporal

import io.circe.syntax._

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.write

case class HadoopWrite(
  node: Node[MultibandTileLayerRDD[SpaceTimeKey] => (Int, MultibandTileLayerRDD[SpaceTimeKey])],
  arg: write.Hadoop
) extends Write[(Int, MultibandTileLayerRDD[SpaceTimeKey])] {
  def asJson = node.asJson :+ arg.asJson
}