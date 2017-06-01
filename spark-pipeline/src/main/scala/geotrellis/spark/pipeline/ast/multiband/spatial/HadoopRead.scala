package geotrellis.spark.pipeline.ast.multiband.spatial

import geotrellis.raster._
import geotrellis.spark.pipeline.ast.Read
import geotrellis.spark.pipeline.json
import geotrellis.vector._

import org.apache.spark.rdd.RDD

case class HadoopRead(arg: json.SpatialMultibandReadHadoop) extends Read[RDD[(ProjectedExtent, MultibandTile)]] {
  def get: RDD[(ProjectedExtent, MultibandTile)] = arg.eval
  def validate: (Boolean, String) = validation
}
