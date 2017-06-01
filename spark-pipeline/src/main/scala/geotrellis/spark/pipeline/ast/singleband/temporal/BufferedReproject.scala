package geotrellis.spark.pipeline.ast.singleband.temporal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json

case class BufferedReproject(
  node: Node[TileLayerRDD[SpaceTimeKey]],
  arg: json.TransformBufferedReproject
) extends Transform[TileLayerRDD[SpaceTimeKey], (Int, TileLayerRDD[SpaceTimeKey])] {
  def get: (Int, TileLayerRDD[SpaceTimeKey]) = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
