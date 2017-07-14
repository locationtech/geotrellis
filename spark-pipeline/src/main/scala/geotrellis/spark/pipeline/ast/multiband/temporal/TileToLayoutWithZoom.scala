package geotrellis.spark.pipeline.ast.multiband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class TileToLayoutWithZoom(
  node: Node[RDD[(TemporalProjectedExtent, MultibandTile)]],
  arg: transform.TileToLayoutWithZoom
) extends Transform[RDD[(TemporalProjectedExtent, MultibandTile)], (Int, MultibandTileLayerRDD[SpaceTimeKey])] {
  def asJson = node.asJson :+ arg.asJson
  def get(implicit sc: SparkContext): (Int, MultibandTileLayerRDD[SpaceTimeKey]) = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}
