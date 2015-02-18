package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.op.local.Add

trait AddRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int): RasterRDD[K] =
    rasterRDD.mapPairs { case (t, r) => (t, Add(r, i)) }

  /** Add a constant Int value to each cell. */
  def +(i: Int): RasterRDD[K] = localAdd(i)

  /** Add a constant Int value to each cell. */
  def +:(i: Int): RasterRDD[K] = localAdd(i)

  /** Add a constant Double value to each cell. */
  def localAdd(d: Double): RasterRDD[K] =
    rasterRDD.mapPairs { case (t, r) => (t, Add(r, d)) }

  /** Add a constant Double value to each cell. */
  def +(d: Double): RasterRDD[K] = localAdd(d)

  /** Add a constant Double value to each cell. */
  def +:(d: Double): RasterRDD[K] = localAdd(d)

  /** Add the values of each cell in each raster.  */
  def localAdd(other: RasterRDD[K]): RasterRDD[K] =
    rasterRDD.combineTiles(other) { case (t1, t2) => Add(t1, t2) }

  /** Add the values of each cell in each raster. */
  def +(other: RasterRDD[K]): RasterRDD[K] = localAdd(other)

  def localAdd(others: Traversable[RasterRDD[K]]): RasterRDD[K] =
    rasterRDD
      .combinePairs(others.toSeq) { case tiles: Seq[(K, Tile)] =>
        (tiles.head.id, Add(tiles.map(_.tile)))
      }

  def +(others: Traversable[RasterRDD[K]]): RasterRDD[K] = localAdd(others)
}
