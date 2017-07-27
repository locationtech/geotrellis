package geotrellis.spark.pipeline.eval.ast.singleband.spatial

import io.circe.syntax._

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.write

case class FileWrite(
  node: Node[TileLayerRDD[SpatialKey] => (Int, TileLayerRDD[SpatialKey])],
  arg: write.File
) extends Write[(Int, TileLayerRDD[SpatialKey])] {
  def asJson = node.asJson :+ arg.asJson
}