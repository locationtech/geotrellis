package geotrellis.spark.pipeline.ast.singleband.temporal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json
import org.apache.spark.rdd.RDD

case class BufferedReproject(
  node: Node[RDD[(TemporalProjectedExtent, Tile)]],
  reproject: json.TransformBufferedReproject
) extends Transform[RDD[(TemporalProjectedExtent, Tile)], TileLayerRDD[SpaceTimeKey]] {
  def get: TileLayerRDD[SpaceTimeKey] = ???
}
