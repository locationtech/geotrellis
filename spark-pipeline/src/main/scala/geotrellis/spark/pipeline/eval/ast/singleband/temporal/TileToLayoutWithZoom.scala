package geotrellis.spark.pipeline.eval.ast.singleband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class TileToLayoutWithZoom(
  node: Node[RDD[(TemporalProjectedExtent, Tile)] => RDD[(TemporalProjectedExtent, Tile)]],
  arg: transform.TileToLayoutWithZoom
) extends Transform[RDD[(TemporalProjectedExtent, Tile)], (Int, TileLayerRDD[SpaceTimeKey])] {
  def asJson = node.asJson :+ arg.asJson
}
