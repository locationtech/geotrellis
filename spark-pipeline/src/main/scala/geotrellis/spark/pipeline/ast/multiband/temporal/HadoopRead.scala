package geotrellis.spark.pipeline.ast.multiband.temporal

import geotrellis.raster._
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.pipeline.ast.Read
import geotrellis.spark.pipeline.json.read
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class HadoopRead(arg: read.TemporalMultibandHadoop) extends Read[RDD[(TemporalProjectedExtent, MultibandTile)]] {
  def get(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = arg.eval
  def validate: (Boolean, String) = validation
}
