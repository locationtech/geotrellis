package geotrellis.spark.pipeline.ast.multiband.spatial

import io.circe.syntax._
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.vector._

import org.apache.spark.SparkContext

case class RetileToLayout(
  node: Node[MultibandTileLayerRDD[SpatialKey]],
  arg: transform.RetileToLayout
) extends Transform[MultibandTileLayerRDD[SpatialKey], MultibandTileLayerRDD[SpatialKey]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] =
    Transform.retileToLayoutSpatial(arg)(node.eval)
}
