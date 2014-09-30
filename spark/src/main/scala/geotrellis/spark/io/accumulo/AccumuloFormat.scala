package geotrellis.spark.io.accumulo

import geotrellis.raster.GridBounds
import geotrellis.spark._
import geotrellis.spark.rdd.{LayerMetaData, RasterRDD}
import geotrellis.spark.tiling._
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD

trait AccumuloFormat[K] {
  /** Map rdd of indexed tiles to tuples of (table name, row mutation) */
  def encode(raster: RasterRDD[K], layer: String): RDD[(Text, Mutation)]

  /** Maps RDD of Accumulo specific Key, Value pairs to a tuple of (K, Tile) and wraps it in RasterRDD */
  def decode(rdd: RDD[(Key, Value)], metaData: LayerMetaData): RasterRDD[K]

  /** List of filters passed by the user. AccumuloFilter is a blank trait, so it's the job of the format
    * to figure out which filters it is going to respect and how. */
  def setFilters(job: Job, metaData: LayerMetaData, filters: Seq[AccumuloFilter]): Unit
}


trait AccumuloFilter

case class SpaceFilter(bounds: TileBounds, scheme: TileCoordScheme) extends AccumuloFilter
object SpaceFilter {
  def apply(col: Int, row: Int, scheme: TileCoordScheme): SpaceFilter =
    SpaceFilter(GridBounds(col, row, col, row), scheme = GridCoordScheme)
}

case class TimeFilter(startTime: Double, endTime: Double) extends AccumuloFilter
object TimeFilter {
  def apply(time: Double): TimeFilter = TimeFilter(time, time)
}
