package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Xor
import geotrellis.spark.rdd.RasterRDD

trait XorRasterRDDMethods extends RasterRDDMethods {
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Xor(r, i))
  }
  /** Xor a constant Int value to each cell. */
  def ^(i: Int): RasterRDD = localXor(i)
  /** Xor a constant Int value to each cell. */
  def ^:(i: Int): RasterRDD = localXor(i)
  /** Xor the values of each cell in each raster.  */
  def localXor(other: RasterRDD): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, Xor(r1, r2))
  }
  /** Xor the values of each cell in each raster. */
  def ^(r: RasterRDD): RasterRDD = localXor(r)
  /** Xor the values of each cell in each raster. */
  def localXor(others: Seq[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, Xor(tmsTiles.map(_.tile)))
    }
  /** Xor the values of each cell in each raster. */
  def ^(others: Seq[RasterRDD]): RasterRDD = localXor(others)
}
