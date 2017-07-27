package geotrellis.spark.pipeline.eval.ast.singleband.temporal

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.pipeline.eval.ast.Read
import geotrellis.spark.pipeline.json.read

import org.apache.spark.rdd.RDD

case class HadoopRead(arg: read.TemporalHadoop) extends Read[RDD[(TemporalProjectedExtent, Tile)]] {
  def asJson = arg.asJson :: Nil
}
