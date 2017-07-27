package geotrellis.spark.pipeline.eval.ast.singleband.spatial

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.vector._

import org.apache.spark.rdd.RDD

case class PerTileReproject(
  node: Node[RDD[(ProjectedExtent, Tile)]],
  arg: transform.PerTileReproject
) extends Transform[RDD[(ProjectedExtent, Tile)], RDD[(ProjectedExtent, Tile)]] {
  def asJson = node.asJson :+ arg.asJson
}
