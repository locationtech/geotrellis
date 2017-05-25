package geotrellis.spark.pipeline.ast.multiband.temporal

import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class FileWrite(
  node: Node[MultibandTileLayerRDD[SpaceTimeKey]],
  write: json.WriteFile
) extends Write[MultibandTileLayerRDD[SpaceTimeKey]] {
  def get: MultibandTileLayerRDD[SpaceTimeKey] = ???
}