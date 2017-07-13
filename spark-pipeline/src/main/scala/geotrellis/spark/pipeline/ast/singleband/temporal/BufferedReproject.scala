package geotrellis.spark.pipeline.ast.singleband.temporal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import org.apache.spark.SparkContext

case class BufferedReproject(
  node: Node[TileLayerRDD[SpaceTimeKey]],
  arg: transform.BufferedReproject
) extends Transform[TileLayerRDD[SpaceTimeKey], (Int, TileLayerRDD[SpaceTimeKey])] {
  def get(implicit sc: SparkContext): (Int, TileLayerRDD[SpaceTimeKey]) = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
