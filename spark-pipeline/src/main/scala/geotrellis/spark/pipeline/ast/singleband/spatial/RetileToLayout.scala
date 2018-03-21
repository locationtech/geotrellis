package geotrellis.spark.pipeline.ast.singleband.spatial

import io.circe.syntax._
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.SparkContext

case class RetileToLayout(
  node: Node[TileLayerRDD[SpatialKey]],
  arg: transform.RetileToLayout
) extends Transform[TileLayerRDD[SpatialKey], TileLayerRDD[SpatialKey]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): TileLayerRDD[SpatialKey] =
    Transform.retileToLayoutSpatial(arg)(node.eval)
}
