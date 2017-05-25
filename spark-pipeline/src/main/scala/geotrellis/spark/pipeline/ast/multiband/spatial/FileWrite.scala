package geotrellis.spark.pipeline.ast.multiband.spatial

import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class FileWrite(
  node: Node[MultibandTileLayerRDD[SpatialKey]],
  write: json.WriteFile
) extends Write[MultibandTileLayerRDD[SpatialKey]] {
  def get: MultibandTileLayerRDD[SpatialKey] = ???
}