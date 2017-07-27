package geotrellis.spark.pipeline.eval.ast.multiband.spatial

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.vector.ProjectedExtent

import org.apache.spark.rdd.RDD

case class BufferedReproject(
  node: Node[RDD[(ProjectedExtent, MultibandTile)] => MultibandTileLayerRDD[SpatialKey]],
  arg: transform.BufferedReproject
) extends Transform[MultibandTileLayerRDD[SpatialKey], (Int, MultibandTileLayerRDD[SpatialKey])] {
  def asJson = node.asJson :+ arg.asJson
}
