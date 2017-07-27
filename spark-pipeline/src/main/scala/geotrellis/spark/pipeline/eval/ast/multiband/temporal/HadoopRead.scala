package geotrellis.spark.pipeline.eval.ast.multiband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.pipeline.eval.ast.Read
import geotrellis.spark.pipeline.json.read

import org.apache.spark.rdd.RDD

case class HadoopRead(arg: read.TemporalMultibandHadoop) extends Read[RDD[(TemporalProjectedExtent, MultibandTile)]] {
  def asJson = arg.asJson :: Nil
}
