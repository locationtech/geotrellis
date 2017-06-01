package geotrellis.spark.pipeline.ast.multiband.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class FileWrite(
  node: Node[(Int, MultibandTileLayerRDD[SpatialKey])],
  arg: json.WriteFile
) extends Write[(Int, MultibandTileLayerRDD[SpatialKey])] {
  def get: (Int, MultibandTileLayerRDD[SpatialKey]) = arg.eval(node.get)
}