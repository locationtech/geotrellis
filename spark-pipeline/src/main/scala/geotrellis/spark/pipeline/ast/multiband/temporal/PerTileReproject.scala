package geotrellis.spark.pipeline.ast.multiband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class PerTileReproject(
  node: Node[RDD[(TemporalProjectedExtent, MultibandTile)]],
  arg: transform.Reproject
) extends Transform[RDD[(TemporalProjectedExtent, MultibandTile)], RDD[(TemporalProjectedExtent, MultibandTile)]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = Transform.perTileReproject(arg)(node.eval)
}
