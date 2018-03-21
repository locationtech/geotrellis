package geotrellis.spark.pipeline.ast.singleband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class TileToLayout(
  node: Node[RDD[(TemporalProjectedExtent, Tile)]],
  arg: transform.TileToLayout
) extends Transform[RDD[(TemporalProjectedExtent, Tile)], TileLayerRDD[SpaceTimeKey]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): TileLayerRDD[SpaceTimeKey] = Transform.tileToLayout(arg)(node.eval)
}
