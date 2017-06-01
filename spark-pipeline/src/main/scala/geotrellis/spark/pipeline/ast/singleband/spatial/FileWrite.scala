package geotrellis.spark.pipeline.ast.singleband.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class FileWrite(
  node: Node[(Int, TileLayerRDD[SpatialKey])],
  arg: json.WriteFile
) extends Write[(Int, TileLayerRDD[SpatialKey])] {
  def get: (Int, TileLayerRDD[SpatialKey]) = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}