package geotrellis.spark.pipeline.ast.multiband.spatial

import geotrellis.spark.pipeline.ast._
import geotrellis.spark._
import geotrellis.spark.pipeline.json

case class HadoopWrite(
  node: Node[MultibandTileLayerRDD[SpatialKey]],
  write: json.WriteHadoop
) extends Write[MultibandTileLayerRDD[SpatialKey]] {
  def get: MultibandTileLayerRDD[SpatialKey] = ???
}