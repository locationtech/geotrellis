package geotrellis.spark.pipeline.eval.ast.multiband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.rdd.RDD

case class TileToLayoutWithZoom(
  node: Node[RDD[(TemporalProjectedExtent, MultibandTile)] => RDD[(TemporalProjectedExtent, MultibandTile)]],
  arg: transform.TileToLayoutWithZoom
) extends Transform[RDD[(TemporalProjectedExtent, MultibandTile)], (Int, MultibandTileLayerRDD[SpaceTimeKey])] {
  def asJson = node.asJson :+ arg.asJson
}
