package geotrellis.spark.pipeline

import geotrellis.raster.CellSize
import geotrellis.spark.Metadata
import org.apache.spark.rdd.RDD

trait Transform extends PipelineExpr

/** Rename Inputs into groups */
case class TransformGroup(
  tags: List[String],
  tag: Option[String],
  `type`: String = "transform.group"
) extends Transform {
  def eval[I, V](rdds: List[RDD[(I, V)]]): List[RDD[(I, V)]] = null
}

/** Merge inputs into a single Multiband RDD */
case class TransformMerge(
  tags: List[String],
  tag: String,
  `type`: String = "transform.merge"
) extends Transform {
  def eval[I, V, V2](rdds: List[RDD[(I, V)]]): List[RDD[(I, V2)]] = null
}

case class TransformMap(
  func: String, // function name
  tag: Option[String] = None,
  `type`: String = "transform.map"
) extends Transform {
  def eval[I, V](rdd: RDD[(I, V)]): RDD[(I, V)] = null
}

case class TransformBufferedReproject(
  crs: String,
  `type`: String = "transform.reproject.buffered"
) extends Transform {
  def eval[I, K, V, M[_]](rdd: RDD[(I, V)]): RDD[(K, V)] with Metadata[M[K]] = null
}

case class TransformPerTileReproject(
  crs: String,
  `type`: String = "transform.reproject.per-tile"
) extends Transform {
  def eval[K, V, M[_]](rdd: RDD[(K, V)] with Metadata[M[K]]): RDD[(K, V)] with Metadata[M[K]] = null
}

case class TransformTile(
  resampleMethod: String = "nearest-neighbor", // nearest-neighbor | bilinear | cubic-convolution | cubic-spline | lanczos,
  layoutScheme: String = "zoomed", // floating | zoomed
  tileSize: Option[Int] = None,
  cellSize: Option[CellSize] = None,
  partitions: Option[Int] = None,
  `type`: String = "transform.tile"
) extends Transform {

}
