package geotrellis.spark.pipeline.ast.multiband.spatial

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json
import geotrellis.vector._
import org.apache.spark.rdd.RDD

case class TileToLayout(
  node: Node[RDD[(ProjectedExtent, MultibandTile)]],
  arg: json.TransformTile
) extends Transform[RDD[(ProjectedExtent, MultibandTile)], MultibandTileLayerRDD[SpatialKey]] {
  def get: MultibandTileLayerRDD[SpatialKey] = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
