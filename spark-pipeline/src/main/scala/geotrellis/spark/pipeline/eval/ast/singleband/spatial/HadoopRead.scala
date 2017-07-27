package geotrellis.spark.pipeline.eval.ast.singleband.spatial

import io.circe.syntax._

import geotrellis.raster._
import geotrellis.spark.pipeline.eval.ast.Read
import geotrellis.spark.pipeline.json.read
import geotrellis.vector._

import org.apache.spark.rdd.RDD

case class HadoopRead(arg: read.SpatialHadoop) extends Read[RDD[(ProjectedExtent, Tile)]] {
  def asJson = arg.asJson :: Nil
}
