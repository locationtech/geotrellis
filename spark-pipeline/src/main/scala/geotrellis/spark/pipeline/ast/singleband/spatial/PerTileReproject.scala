package geotrellis.spark.pipeline.ast.singleband.spatial

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json
import geotrellis.vector._
import org.apache.spark.rdd.RDD

case class PerTileReproject(
  node: Node[RDD[(ProjectedExtent, Tile)]],
  reproject: json.TransformPerTileReproject
) extends Transform[RDD[(ProjectedExtent, Tile)], TileLayerRDD[SpatialKey]] {
  def get: TileLayerRDD[SpatialKey] = ???
}
