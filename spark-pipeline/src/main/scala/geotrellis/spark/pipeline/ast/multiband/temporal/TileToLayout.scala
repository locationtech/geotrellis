package geotrellis.spark.pipeline.ast.multiband.temporal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json
import geotrellis.vector._
import org.apache.spark.rdd.RDD

case class TileToLayout(
  node: Node[RDD[(TemporalProjectedExtent, MultibandTile)]],
  arg: json.TransformTile
) extends Transform[RDD[(TemporalProjectedExtent, MultibandTile)], MultibandTileLayerRDD[SpaceTimeKey]] {
  def get: MultibandTileLayerRDD[SpaceTimeKey] = arg.eval(node.get)
}
