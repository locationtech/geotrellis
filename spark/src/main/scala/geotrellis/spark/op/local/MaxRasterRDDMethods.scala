package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Max
import geotrellis.raster.Tile

trait MaxRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Max a constant Int value to each cell. */
  def localMax(i: Int): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Max(r, i))
  }
  /** Max a constant Double value to each cell. */
  def localMax(d: Double): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Max(r, d))
  }
  /** Max the values of each cell in each raster.  */
  def localMax(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = rasterRDD.combineTiles(other) {
    case (t1, t2) => Max(t1, t2)
  }
  /** Max the values of each cell in each raster.  */
  def localMax(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] =
    rasterRDD.combinePairs(others) {
      case tiles =>
        (tiles.head.id, Max(tiles.map(_.tile)))
    }
}
