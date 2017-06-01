package geotrellis.spark.pipeline.ast.multiband.temporal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class BufferedReproject(
  node: Node[MultibandTileLayerRDD[SpaceTimeKey]],
  arg: json.TransformBufferedReproject
) extends Transform[MultibandTileLayerRDD[SpaceTimeKey], (Int, MultibandTileLayerRDD[SpaceTimeKey])] {
  def get: (Int, MultibandTileLayerRDD[SpaceTimeKey]) = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
