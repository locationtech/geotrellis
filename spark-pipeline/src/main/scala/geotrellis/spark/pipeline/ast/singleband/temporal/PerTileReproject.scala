package geotrellis.spark.pipeline.ast.singleband.temporal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json
import org.apache.spark.rdd.RDD

case class PerTileReproject(
  node: Node[RDD[(TemporalProjectedExtent, Tile)]],
  arg: json.TransformPerTileReproject
) extends Transform[RDD[(TemporalProjectedExtent, Tile)], RDD[(TemporalProjectedExtent, Tile)]] {
  def get: RDD[(TemporalProjectedExtent, Tile)] = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
