package geotrellis.spark.pipeline.ast.singleband.spatial

import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class HadoopWrite(
  node: Node[TileLayerRDD[SpatialKey]],
  write: json.WriteHadoop
) extends Write[TileLayerRDD[SpatialKey]] {
  def get: TileLayerRDD[SpatialKey] = ???
}