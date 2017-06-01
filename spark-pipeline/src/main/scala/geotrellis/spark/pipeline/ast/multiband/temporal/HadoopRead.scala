package geotrellis.spark.pipeline.ast.multiband.temporal

import geotrellis.raster._
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.pipeline.ast.Read
import geotrellis.spark.pipeline.json
import geotrellis.vector._

import org.apache.spark.rdd.RDD

case class HadoopRead(arg: json.TemporalMultibandReadHadoop) extends Read[RDD[(TemporalProjectedExtent, MultibandTile)]] {
  def get: RDD[(TemporalProjectedExtent, MultibandTile)] = arg.eval
  def validate: (Boolean, String) = validation
}
