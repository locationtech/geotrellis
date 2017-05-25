package geotrellis.spark.pipeline.ast.singleband.temporal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast.Read
import geotrellis.spark.pipeline.json
import org.apache.spark.rdd.RDD

case class FileRead(read: json.ReadFile) extends Read[RDD[(TemporalProjectedExtent, Tile)]] {
  def get: RDD[(TemporalProjectedExtent, Tile)] = ???
}
