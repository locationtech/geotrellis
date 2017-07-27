package geotrellis.spark.pipeline.eval.ast.multiband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.rdd.RDD

case class BufferedReproject(
  node: Node[RDD[(TemporalProjectedExtent, MultibandTile)] => MultibandTileLayerRDD[SpaceTimeKey]],
  arg: transform.BufferedReproject
) extends Transform[MultibandTileLayerRDD[SpaceTimeKey], (Int, MultibandTileLayerRDD[SpaceTimeKey])] {
  def asJson = node.asJson :+ arg.asJson
}
