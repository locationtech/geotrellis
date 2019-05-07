package geotrellis.layers.mapalgebra.local

import geotrellis.raster.mapalgebra.local.Minority
import geotrellis.raster.{DI, Tile}
import geotrellis.layers._
import geotrellis.util.MethodExtensions

trait MinorityTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others)(Minority.apply)

  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(rs: Seq[(K, Tile)]*)(implicit d: DI): Seq[(K, Tile)] =
    localMinority(rs)

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others) { tiles => Minority(n, tiles) }

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, rs: Seq[(K, Tile)]*)(implicit d: DI): Seq[(K, Tile)] =
    localMinority(n, rs)
}
