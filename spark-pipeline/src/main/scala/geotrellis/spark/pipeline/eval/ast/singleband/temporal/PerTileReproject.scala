package geotrellis.spark.pipeline.eval.ast.singleband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.rdd.RDD

case class PerTileReproject(
  node: Node[RDD[(TemporalProjectedExtent, Tile)]],
  arg: transform.PerTileReproject
) extends Transform[RDD[(TemporalProjectedExtent, Tile)], RDD[(TemporalProjectedExtent, Tile)]] {
  def asJson = node.asJson :+ arg.asJson
}
