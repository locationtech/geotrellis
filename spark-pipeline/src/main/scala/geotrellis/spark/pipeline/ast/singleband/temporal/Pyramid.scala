package geotrellis.spark.pipeline.ast.singleband.temporal

import io.circe.syntax._
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class Pyramid(
  node: Node[TileLayerRDD[SpaceTimeKey]],
  arg: transform.Pyramid
) extends Transform[TileLayerRDD[SpaceTimeKey], Stream[(Int, TileLayerRDD[SpaceTimeKey])]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): Stream[(Int, TileLayerRDD[SpaceTimeKey])] =
    Transform.pyramid(arg)(node.eval)
}
