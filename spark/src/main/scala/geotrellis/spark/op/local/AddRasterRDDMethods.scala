package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Add
import geotrellis.spark.rdd.RasterRDD

trait AddRasterRDDMethods extends RasterRDDMethods {
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Add(r, i)) }
  /** Add a constant Int value to each cell. */
  def +(i: Int): RasterRDD = localAdd(i)
  /** Add a constant Int value to each cell. */
  def +:(i: Int): RasterRDD = localAdd(i)
  /** Add a constant Double value to each cell. */
  def localAdd(d: Double): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Add(r, d)) }
  /** Add a constant Double value to each cell. */
  def +(d: Double): RasterRDD = localAdd(d)
  /** Add a constant Double value to each cell. */
  def +:(d: Double): RasterRDD = localAdd(d)
  /** Add the values of each cell in each raster.  */
  def localAdd(other: RasterRDD): RasterRDD =
    rasterRDD.combineTiles(other) {
      case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, Add(r1, r2))
    }
  /** Add the values of each cell in each raster. */
  def +(other: RasterRDD): RasterRDD = localAdd(other)

  def localAdd(others: Traversable[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, Add(tmsTiles.map(_.tile)))
    }

  def +(others: Traversable[RasterRDD]): RasterRDD = localAdd(others)
}
