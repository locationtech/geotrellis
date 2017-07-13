package geotrellis.spark.pipeline.ast.singleband.spatial

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.vector._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class TileToLayoutWithZoom(
  node: Node[RDD[(ProjectedExtent, Tile)]],
  arg: transform.TileToLayoutWithZoom
) extends Transform[RDD[(ProjectedExtent, Tile)], (Int, TileLayerRDD[SpatialKey])] {
  def get(implicit sc: SparkContext): (Int, TileLayerRDD[SpatialKey]) = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
