package geotrellis.spark.pipeline.ast.multiband.spatial

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class BufferedReproject(
  node: Node[MultibandTileLayerRDD[SpatialKey]],
  arg: json.TransformBufferedReproject
) extends Transform[MultibandTileLayerRDD[SpatialKey], (Int, MultibandTileLayerRDD[SpatialKey])] {
  def get: (Int, MultibandTileLayerRDD[SpatialKey]) = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
