package geotrellis.spark.pipeline.ast.multiband.temporal

import io.circe.syntax._

import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class TileToLayout(
  node: Node[RDD[(TemporalProjectedExtent, MultibandTile)]],
  arg: transform.TileToLayout
) extends Transform[RDD[(TemporalProjectedExtent, MultibandTile)], MultibandTileLayerRDD[SpaceTimeKey]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): MultibandTileLayerRDD[SpaceTimeKey] = Transform.tileToLayout(arg)(node.eval)
}
