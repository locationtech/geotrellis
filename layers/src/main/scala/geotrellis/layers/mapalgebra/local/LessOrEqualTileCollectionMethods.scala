package geotrellis.layers.mapalgebra.local

import geotrellis.raster.mapalgebra.local.LessOrEqual
import geotrellis.raster.Tile
import geotrellis.layers._
import geotrellis.util.MethodExtensions


trait LessOrEqualTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than or equal to
    * the input integer, else 0.
    */
  def localLessOrEqual(i: Int) =
    self.mapValues { r => LessOrEqual(r, i) }
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than or equal to
    * the input integer, else 0.
    */
  def localLessOrEqualRightAssociative(i: Int) =
    self.mapValues { r => LessOrEqual(i, r) }
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than or equal to
    * the input integer, else 0.
    */
  def <=(i: Int) = localLessOrEqual(i)
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than or equal to
    * the input integer, else 0.
    */
  def <=:(i: Int) = localLessOrEqualRightAssociative(i)
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than or equal to
    * the input double, else 0.
    */
  def localLessOrEqual(d: Double) =
    self.mapValues { r => LessOrEqual(r, d) }
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than or equal to
    * the input double, else 0.
    */
  def localLessOrEqualRightAssociative(d: Double) =
    self.mapValues { r => LessOrEqual(d, r) }
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than or equal to
    * the input double, else 0.
    */
  def <=(d: Double) = localLessOrEqual(d)
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than or equal to
    * the input double, else 0.
    */
  def <=:(d: Double) = localLessOrEqualRightAssociative(d)
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the rasters are less than or equal to the
    * next raster, else 0.
    */
  def localLessOrEqual(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(LessOrEqual.apply)

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the rasters are less than or equal to the
    * next raster, else 0.
    */
  def <=(other: Seq[(K, Tile)]) = localLessOrEqual(other)
}
