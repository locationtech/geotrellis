package geotrellis.layers.mapalgebra.local

import geotrellis.raster.Tile
import geotrellis.raster.mapalgebra.local.Unequal
import geotrellis.layers._
import geotrellis.util.MethodExtensions

trait UnequalTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def localUnequal(i: Int) =
    self.mapValues { r => Unequal(r, i) }

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def !==(i: Int) = localUnequal(i)

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def !==:(i: Int) = localUnequal(i)

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def localUnequal(d: Double) =
    self.mapValues { r => Unequal(r, d) }

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def !==(d: Double) = localUnequal(d)

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def !==:(d: Double) = localUnequal(d)

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell valued of the rasters are not equal, else 0.
   */
  def localUnequal(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Unequal.apply)

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell valued of the raster are not equal, else 0.
   */
  def !==(other: Seq[(K, Tile)]) = localUnequal(other)
}
