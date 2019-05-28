package geotrellis.layers.mapalgebra.local

import geotrellis.raster.mapalgebra.local.And
import geotrellis.raster.Tile
import geotrellis.layers._
import geotrellis.util.MethodExtensions


trait AndTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /** And a constant Int value to each cell. */
  def localAnd(i: Int) =
    self.mapValues { r => And(r, i) }

  /** And a constant Int value to each cell. */
  def &(i: Int) = localAnd(i)

  /** And a constant Int value to each cell. */
  def &:(i: Int) = localAnd(i)

  /** And the values of each cell in each raster.  */
  def localAnd(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other){ And.apply }

  /** And the values of each cell in each raster. */
  def &(rs: TileLayerCollection[K]): Seq[(K, Tile)] = localAnd(rs)

  /** And the values of each cell in each raster.  */
  def localAnd(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others){ And.apply }

  /** And the values of each cell in each raster. */
  def &(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] = localAnd(others)
}
