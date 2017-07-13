package geotrellis.spark.pipeline.ast.multiband.spatial

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.vector._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class TileToLayoutWithZoom(
  node: Node[RDD[(ProjectedExtent, MultibandTile)]],
  arg: transform.TileToLayoutWithZoom
) extends Transform[RDD[(ProjectedExtent, MultibandTile)],(Int, MultibandTileLayerRDD[SpatialKey])] {
  def get(implicit sc: SparkContext): (Int, MultibandTileLayerRDD[SpatialKey]) = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
