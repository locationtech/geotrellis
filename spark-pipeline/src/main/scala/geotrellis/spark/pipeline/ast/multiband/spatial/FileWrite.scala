package geotrellis.spark.pipeline.ast.multiband.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.write

import org.apache.spark.SparkContext

case class FileWrite(
  node: Node[(Int, MultibandTileLayerRDD[SpatialKey])],
  arg: write.File
) extends Write[(Int, MultibandTileLayerRDD[SpatialKey])] {
  def get(implicit sc: SparkContext): (Int, MultibandTileLayerRDD[SpatialKey]) = arg.eval(node.get)
  def validate: (Boolean, String) = {
    val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
    else node.validation
    val (fs, msgs) = validation
    (f && fs, msgs ++ msg)
  }
}