package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Min
import geotrellis.raster.Tile

trait MinRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Min a constant Int value to each cell. */
  def localMin(i: Int): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Min(r, i))
  }
  /** Min a constant Double value to each cell. */
  def localMin(d: Double): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Min(r, d))
  }
  /** Min the values of each cell in each raster.  */
  def localMin(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = rasterRDD.combineTiles(other) {
    case (t1, t2) => Min(t1, t2)
  }
  /** Min the values of each cell in each raster.  */
  def localMin(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] =
    rasterRDD.combinePairs(others.toSeq) {
      case tiles =>
        (tiles.head.id, Min(tiles.map(_.tile)))
    }
}
