package geotrellis.spark.mapalgebra.local

import geotrellis.spark.mapalgebra._
import geotrellis.raster.mapalgebra.local._
import geotrellis.spark.mapalgebra.Implicits._
import geotrellis.raster._

import org.apache.spark.sql.Dataset

trait AddTileDatasetMethods[K] extends TileDatasetMethods[K] {
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int) = self.mapValues { r => Add(r, i) }

  /** Add a constant Int value to each cell. */
  def +(i: Int) = localAdd(i)

  /** Add a constant Int value to each cell. */
  def +:(i: Int) = localAdd(i)

  /** Add a constant Double value to each cell. */
  def localAdd(d: Double) = self.mapValues { r => Add(r, d) }

  /** Add a constant Double value to each cell. */
  def +(d: Double) = localAdd(d)

  /** Add a constant Double value to each cell. */
  def +:(d: Double) = localAdd(d)

  /** Add the values of each cell in each raster.  */
  def localAdd(other: Dataset[(K, Tile)]): Dataset[(K, Tile)] = self.combineValues(other) { Add.apply }

  /** Add the values of each cell in each raster. */
  def +(other: Dataset[(K, Tile)]): Dataset[(K, Tile)] = localAdd(other)

  def localAdd(others: Traversable[Dataset[(K, Tile)]]): Dataset[(K, Tile)] = self.combineValues(others) { Add.apply }

  def +(others: Traversable[Dataset[(K, Tile)]]): Dataset[(K, Tile)] = localAdd(others)
}
