package geotrellis.spark.pipeline.eval.ast.multiband.spatial

import io.circe.syntax._

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.write
import geotrellis.vector.ProjectedExtent

import org.apache.spark.rdd.RDD

case class FileWritePerTile(
  node: Node[RDD[(ProjectedExtent, MultibandTile)] => (Int, MultibandTileLayerRDD[SpatialKey])],
  arg: write.File
) extends Write[(Int, MultibandTileLayerRDD[SpatialKey])] {
  def asJson = node.asJson :+ arg.asJson
}