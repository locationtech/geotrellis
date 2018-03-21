package geotrellis.spark.pipeline.ast.singleband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class PerTileReproject(
  node: Node[RDD[(TemporalProjectedExtent, Tile)]],
  arg: transform.Reproject
) extends Transform[RDD[(TemporalProjectedExtent, Tile)], RDD[(TemporalProjectedExtent, Tile)]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = Transform.perTileReproject(arg)(node.eval)
}
