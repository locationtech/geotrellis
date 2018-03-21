package geotrellis.spark.pipeline.ast.multiband.temporal

import io.circe.syntax._
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import org.apache.spark.SparkContext

case class RetileToLayout(
  node: Node[MultibandTileLayerRDD[SpaceTimeKey]],
  arg: transform.RetileToLayout
) extends Transform[MultibandTileLayerRDD[SpaceTimeKey], MultibandTileLayerRDD[SpaceTimeKey]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): MultibandTileLayerRDD[SpaceTimeKey] =
    Transform.retileToLayoutTemporal(arg)(node.eval)
}
