package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Xor
import geotrellis.raster.Tile

trait XorRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Xor(r, i))
  }
  /** Xor a constant Int value to each cell. */
  def ^(i: Int): RasterRDD[K, Tile] = localXor(i)
  /** Xor a constant Int value to each cell. */
  def ^:(i: Int): RasterRDD[K, Tile] = localXor(i)
  /** Xor the values of each cell in each raster.  */
  def localXor(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = rasterRDD.combineTiles(other) {
    case (t1, t2) => Xor(t1, t2)
  }
  /** Xor the values of each cell in each raster. */
  def ^(r: RasterRDD[K, Tile]): RasterRDD[K, Tile] = localXor(r)
  /** Xor the values of each cell in each raster. */
  def localXor(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] =
    rasterRDD.combinePairs(others.toSeq) {
      case tiles =>
        (tiles.head.id, Xor(tiles.map(_.tile)))
    }
  /** Xor the values of each cell in each raster. */
  def ^(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] = localXor(others)
}
