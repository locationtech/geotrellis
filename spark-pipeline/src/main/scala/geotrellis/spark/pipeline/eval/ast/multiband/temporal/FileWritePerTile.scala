package geotrellis.spark.pipeline.eval.ast.multiband.temporal

import io.circe.syntax._

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.write

import org.apache.spark.rdd.RDD

case class FileWritePerTile(
  node: Node[RDD[(TemporalProjectedExtent, MultibandTile)] => (Int, MultibandTileLayerRDD[SpaceTimeKey])],
  arg: write.File
) extends Write[(Int, MultibandTileLayerRDD[SpaceTimeKey])] {
  def asJson = node.asJson :+ arg.asJson
}