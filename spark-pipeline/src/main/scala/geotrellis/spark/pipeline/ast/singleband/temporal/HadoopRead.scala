package geotrellis.spark.pipeline.ast.singleband.temporal

import geotrellis.raster._
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.pipeline.ast.Read
import geotrellis.spark.pipeline.json.read

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class HadoopRead(arg: read.TemporalHadoop) extends Read[RDD[(TemporalProjectedExtent, Tile)]] {
  def get(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = arg.eval
  def validate: (Boolean, String) = validation
}
