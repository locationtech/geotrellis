package geotrellis.spark.pipeline.ast.singleband.spatial

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.vector._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class PerTileReproject(
  node: Node[RDD[(ProjectedExtent, Tile)]],
  arg: transform.Reproject
) extends Transform[RDD[(ProjectedExtent, Tile)], RDD[(ProjectedExtent, Tile)]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = Transform.perTileReproject(arg)(node.eval)
}
