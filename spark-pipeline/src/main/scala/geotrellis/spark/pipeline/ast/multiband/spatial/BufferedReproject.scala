package geotrellis.spark.pipeline.ast.multiband.spatial

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.SparkContext

case class BufferedReproject(
  node: Node[MultibandTileLayerRDD[SpatialKey]],
  arg: transform.Reproject
) extends Transform[MultibandTileLayerRDD[SpatialKey], MultibandTileLayerRDD[SpatialKey]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] = Transform.bufferedReproject(arg)(node.eval)
}
