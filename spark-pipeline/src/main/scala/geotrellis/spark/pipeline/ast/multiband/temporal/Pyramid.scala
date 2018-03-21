package geotrellis.spark.pipeline.ast.multiband.temporal

import io.circe.syntax._
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.vector._

import org.apache.spark.SparkContext

case class Pyramid(
  node: Node[MultibandTileLayerRDD[SpaceTimeKey]],
  arg: transform.Pyramid
) extends Transform[MultibandTileLayerRDD[SpaceTimeKey], Stream[(Int, MultibandTileLayerRDD[SpaceTimeKey])]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): Stream[(Int, MultibandTileLayerRDD[SpaceTimeKey])] =
    Transform.pyramid(arg)(node.eval)
}
