package geotrellis.spark.pipeline.ast.multiband.spatial

import geotrellis.spark.pipeline.json
import geotrellis.spark.pipeline.ast._
import geotrellis.spark._
import geotrellis.spark.io._

case class HadoopWrite(
  node: Node[(Int, MultibandTileLayerRDD[SpatialKey])],
  arg: json.WriteHadoop
) extends Write[(Int, MultibandTileLayerRDD[SpatialKey])] {
  def get: (Int, MultibandTileLayerRDD[SpatialKey]) = arg.eval(node.get)
}