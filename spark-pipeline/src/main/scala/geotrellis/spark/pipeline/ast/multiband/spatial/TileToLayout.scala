package geotrellis.spark.pipeline.ast.multiband.spatial

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json
import geotrellis.vector._
import org.apache.spark.rdd.RDD

case class TileToLayout(
  node: Node[RDD[(ProjectedExtent, MultibandTile)]],
  reproject: json.TransformTile
) extends Transform[RDD[(ProjectedExtent, MultibandTile)], MultibandTileLayerRDD[SpatialKey]] {
  def get: MultibandTileLayerRDD[SpatialKey] = ???
}
