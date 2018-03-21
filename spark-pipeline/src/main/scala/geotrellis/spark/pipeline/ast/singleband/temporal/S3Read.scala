package geotrellis.spark.pipeline.ast.singleband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.tiling.TemporalProjectedExtent
import geotrellis.spark.pipeline.ast.Input
import geotrellis.spark.pipeline.json.read

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class S3Read(arg: read.JsonRead) extends Input[RDD[(TemporalProjectedExtent, Tile)]] {
  def asJson = arg.asJson :: Nil
  def eval(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = Input.temporalS3(arg)
}
