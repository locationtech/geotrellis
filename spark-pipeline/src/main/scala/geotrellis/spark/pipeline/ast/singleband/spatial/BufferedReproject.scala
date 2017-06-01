package geotrellis.spark.pipeline.ast.singleband.spatial

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json
import geotrellis.vector._
import org.apache.spark.rdd.RDD

case class BufferedReproject(
  node: Node[TileLayerRDD[SpatialKey]],
  arg: json.TransformBufferedReproject
) extends Transform[RDD[(ProjectedExtent, Tile)], (Int, TileLayerRDD[SpatialKey])] {
  def get: (Int, TileLayerRDD[SpatialKey]) = arg.eval(node.get)
}
