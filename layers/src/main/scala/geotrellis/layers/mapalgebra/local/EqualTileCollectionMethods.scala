package geotrellis.layers.mapalgebra.local

import geotrellis.raster.mapalgebra.local.Equal
import geotrellis.raster.Tile
import geotrellis.layers._
import geotrellis.util.MethodExtensions


trait EqualTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the input
    * integer, else 0.
    */
  def localEqual(i: Int) =
    self.mapValues { r => Equal(r, i) }

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the input
    * double, else 0.
    */
  def localEqual(d: Double) =
    self.mapValues { r => Equal(r, d) }

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the provided
    * raster, else 0.
    */
  def localEqual(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Equal.apply)
}
