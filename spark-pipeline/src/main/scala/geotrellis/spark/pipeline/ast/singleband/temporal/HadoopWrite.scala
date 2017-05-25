package geotrellis.spark.pipeline.ast.singleband.temporal

import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class HadoopWrite(
  node: Node[TileLayerRDD[SpaceTimeKey]],
  write: json.WriteHadoop
) extends Write[TileLayerRDD[SpaceTimeKey]] {
  def get: TileLayerRDD[SpaceTimeKey] = ???
}