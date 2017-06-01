package geotrellis.spark.pipeline.ast.singleband.temporal

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class HadoopWrite(
  node: Node[(Int, TileLayerRDD[SpaceTimeKey])],
  arg: json.WriteHadoop
) extends Write[(Int, TileLayerRDD[SpaceTimeKey])] {
  def get: (Int, TileLayerRDD[SpaceTimeKey]) = arg.eval(node.get)
}