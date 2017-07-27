package geotrellis.spark.pipeline.eval.ast.singleband.temporal

import io.circe.syntax._

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.write

case class FileWrite(
  node: Node[TileLayerRDD[SpaceTimeKey] => (Int, TileLayerRDD[SpaceTimeKey])],
  arg: write.File
) extends Write[(Int, TileLayerRDD[SpaceTimeKey])] {
  def asJson = node.asJson :+ arg.asJson
}