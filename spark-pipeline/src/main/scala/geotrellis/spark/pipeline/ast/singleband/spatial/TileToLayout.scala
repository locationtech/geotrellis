package geotrellis.spark.pipeline.ast.singleband.spatial

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.vector._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class TileToLayout(
  node: Node[RDD[(ProjectedExtent, Tile)]],
  arg: transform.TileToLayout
) extends Transform[RDD[(ProjectedExtent, Tile)], TileLayerRDD[SpatialKey]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): TileLayerRDD[SpatialKey] = Transform.tileToLayout(arg)(node.eval)
}
