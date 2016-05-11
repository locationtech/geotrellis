package geotrellis.spark.split

import geotrellis.raster._
import geotrellis.raster.split.SplitMethods
import geotrellis.raster.split.Split.Options
import geotrellis.spark._
import geotrellis.vector.ProjectedExtent
import geotrellis.util._

import org.apache.spark.rdd.RDD

abstract class ProjectedExtentRDDSplitMethods[K: Component[?, ProjectedExtent], V <: CellGrid: (? => SplitMethods[V])] extends MethodExtensions[RDD[(K, V)]] {
  /** Splits an RDD of tiles into tiles of size (tileCols x tileRows), and updates the ProjectedExtent component of the keys.
    */
  def split(tileCols: Int, tileRows: Int): RDD[(K, V)] =
    Split(self, tileCols, tileRows)
}
