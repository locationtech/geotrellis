package geotrellis.layers.mapalgebra.local

import geotrellis.raster.mapalgebra.local.Majority
import geotrellis.raster.Tile
import geotrellis.layers._
import geotrellis.util.MethodExtensions


trait MajorityTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others)(Majority.apply)

  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(rs: Seq[(K, Tile)]*): Seq[(K, Tile)] =
    localMajority(rs)

  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others) { tiles => Majority(n, tiles) }

  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, rs: Seq[(K, Tile)]*): Seq[(K, Tile)] =
    localMajority(n, rs)
}
