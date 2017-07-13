package geotrellis.spark.pipeline.ast.multiband.temporal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class PerTileReproject(
  node: Node[RDD[(TemporalProjectedExtent, MultibandTile)]],
  arg: transform.PerTileReproject
) extends Transform[RDD[(TemporalProjectedExtent, MultibandTile)], RDD[(TemporalProjectedExtent, MultibandTile)]] {
  def get(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
