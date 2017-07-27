package geotrellis.spark.pipeline.eval.ast.singleband.spatial

import io.circe.syntax._

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json.write
import geotrellis.vector.ProjectedExtent

import org.apache.spark.rdd.RDD

case class HadoopWritePerTile(
  node: Node[RDD[(ProjectedExtent, Tile)] => (Int, TileLayerRDD[SpatialKey])],
  arg: write.Hadoop
) extends Write[(Int, TileLayerRDD[SpatialKey])] {
  def asJson = node.asJson :+ arg.asJson
}
