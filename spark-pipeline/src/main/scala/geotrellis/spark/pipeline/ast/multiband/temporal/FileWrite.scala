package geotrellis.spark.pipeline.ast.multiband.temporal

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class FileWrite(
  node: Node[(Int, MultibandTileLayerRDD[SpaceTimeKey])],
  arg: json.WriteFile
) extends Write[(Int, MultibandTileLayerRDD[SpaceTimeKey])] {
  def get: (Int, MultibandTileLayerRDD[SpaceTimeKey]) = arg.eval(node.get)
}