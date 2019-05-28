package geotrellis.layers.mapalgebra.local

import geotrellis.raster.mapalgebra.local.Less
import geotrellis.raster.Tile
import geotrellis.layers._
import geotrellis.util.MethodExtensions


trait LessTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    */
  def localLess(i: Int) =
    self.mapValues { r => Less(r, i) }

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    */
  def <(i: Int) = localLess(i)

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    */
  def localLessRightAssociative(i: Int) =
    self.mapValues { r => Less(i, r) }

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    *
    * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
    */
  def <<:(i: Int) = localLessRightAssociative(i)

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    */
  def localLess(d: Double) =
    self.mapValues { r => Less(r, d) }

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    */
  def localLessRightAssociative(d: Double) =
    self.mapValues { r => Less(d, r) }
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    */
  def <(d: Double) = localLess(d)
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    *
    * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
    */
  def <<:(d: Double) = localLessRightAssociative(d)
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the rasters are less than the next
    * raster, else 0.
    */
  def localLess(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Less.apply)

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the rasters are less than the next
    * raster, else 0.
    */
  def <(other: Seq[(K, Tile)]): Seq[(K, Tile)] = localLess(other)
}
