package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.op.local.And

trait AndRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** And a constant Int value to each cell. */
  def localAnd(i: Int): RasterRDD[K] = 
    rasterRDD
      .mapPairs { case (t, r) =>
        (t, And(r, i))
      }

  /** And a constant Int value to each cell. */
  def &(i: Int): RasterRDD[K] = localAnd(i)

  /** And a constant Int value to each cell. */
  def &:(i: Int): RasterRDD[K] = localAnd(i)

  /** And the values of each cell in each raster.  */
  def localAnd(other: RasterRDD[K]): RasterRDD[K] =
    rasterRDD.combineValues(other) { case (t1, t2) => And(t1, t2) }

  /** And the values of each cell in each raster. */
  def &(rs: RasterRDD[K]): RasterRDD[K] = localAnd(rs)

  /** And the values of each cell in each raster.  */
  def localAnd(others: Traversable[RasterRDD[K]]): RasterRDD[K] =
    rasterRDD
      .combinePairs(others.toSeq) { case tiles: Seq[(K, Tile)] =>
        (tiles.head.id, And(tiles.map(_.tile)))
      }

  /** And the values of each cell in each raster. */
  def &(others: Traversable[RasterRDD[K]]): RasterRDD[K] = localAnd(others)
}
