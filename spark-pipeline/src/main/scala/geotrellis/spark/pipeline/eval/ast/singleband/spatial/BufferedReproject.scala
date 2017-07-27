package geotrellis.spark.pipeline.eval.ast.singleband.spatial

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.vector._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class BufferedReproject(
  node: Node[RDD[(ProjectedExtent, Tile)] => TileLayerRDD[SpatialKey]],
  arg: transform.BufferedReproject
) extends Transform[TileLayerRDD[SpatialKey], (Int, TileLayerRDD[SpatialKey])] {
  def asJson = node.asJson :+ arg.asJson

}
