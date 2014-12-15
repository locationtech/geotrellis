package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Xor

trait XorRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int): RasterRDD[K] = rasterRDD.mapRows {
    case (t, r) => (t, Xor(r, i))
  }
  /** Xor a constant Int value to each cell. */
  def ^(i: Int): RasterRDD[K] = localXor(i)
  /** Xor a constant Int value to each cell. */
  def ^:(i: Int): RasterRDD[K] = localXor(i)
  /** Xor the values of each cell in each raster.  */
  def localXor(other: RasterRDD[K]): RasterRDD[K] = rasterRDD.combineRows(other) {
    case ((t1, r1), (t2, r2)) => (t1, Xor(r1, r2))
  }
  /** Xor the values of each cell in each raster. */
  def ^(r: RasterRDD[K]): RasterRDD[K] = localXor(r)
  /** Xor the values of each cell in each raster. */
  def localXor(others: Seq[RasterRDD[K]]): RasterRDD[K] =
    rasterRDD.combineRows(others.toSeq) {
      case tiles =>
        (tiles.head.id, Xor(tiles.map(_.tile)))
    }
  /** Xor the values of each cell in each raster. */
  def ^(others: Seq[RasterRDD[K]]): RasterRDD[K] = localXor(others)
}
