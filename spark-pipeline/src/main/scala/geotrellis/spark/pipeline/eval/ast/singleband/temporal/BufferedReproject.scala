package geotrellis.spark.pipeline.eval.ast.singleband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.rdd.RDD

case class BufferedReproject(
  node: Node[RDD[(TemporalProjectedExtent, Tile)] => TileLayerRDD[SpaceTimeKey]],
  arg: transform.BufferedReproject
) extends Transform[TileLayerRDD[SpaceTimeKey], (Int, TileLayerRDD[SpaceTimeKey])] {
  def asJson = node.asJson :+ arg.asJson
}
