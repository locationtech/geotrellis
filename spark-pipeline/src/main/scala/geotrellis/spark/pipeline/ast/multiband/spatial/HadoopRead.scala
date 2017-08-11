package geotrellis.spark.pipeline.ast.multiband.spatial

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark.pipeline.ast.Input
import geotrellis.spark.pipeline.json.read
import geotrellis.vector._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class HadoopRead(arg: read.JsonRead) extends Input[RDD[(ProjectedExtent, MultibandTile)]] {
  def asJson = arg.asJson :: Nil
  def get(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    Input.evalSpatialMultibandHadoop(arg)
}
