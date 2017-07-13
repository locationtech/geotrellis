package geotrellis.spark.pipeline.ast.singleband.temporal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class TileToLayoutWithZoom(
  node: Node[RDD[(TemporalProjectedExtent, Tile)]],
  arg: transform.TileToLayoutWithZoom
) extends Transform[RDD[(TemporalProjectedExtent, Tile)], (Int, TileLayerRDD[SpaceTimeKey])] {
  def get(implicit sc: SparkContext): (Int, TileLayerRDD[SpaceTimeKey]) = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
