package geotrellis.spark.pipeline.eval.ast.singleband.temporal

import io.circe.syntax._

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.write

import org.apache.spark.rdd.RDD

case class HadoopWritePerTile(
  node: Node[RDD[(TemporalProjectedExtent, Tile)] => (Int, TileLayerRDD[SpaceTimeKey])],
  arg: write.Hadoop
) extends Write[(Int, TileLayerRDD[SpaceTimeKey])] {
  def asJson = node.asJson :+ arg.asJson
}