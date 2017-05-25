package geotrellis.spark.pipeline.ast.singleband.spatial

import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class FileWrite(
  node: Node[TileLayerRDD[SpatialKey]],
  write: json.WriteFile
) extends Write[TileLayerRDD[SpatialKey]] {
  def get: TileLayerRDD[SpatialKey] = ???
}