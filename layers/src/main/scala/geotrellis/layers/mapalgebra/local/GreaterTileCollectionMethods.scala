package geotrellis.layers.mapalgebra.local

import geotrellis.raster.mapalgebra.local.Greater
import geotrellis.raster.Tile
import geotrellis.layers._
import geotrellis.util.MethodExtensions


trait GreaterTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def localGreater(i: Int) =
    self.mapValues { r => Greater(r, i) }

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def localGreaterRightAssociative(i: Int) =
    self.mapValues { r => Greater(i, r) }

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def >(i: Int) = localGreater(i)

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    *
    * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
    */
  def >>:(i: Int) = localGreaterRightAssociative(i)

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def localGreater(d: Double) =
    self.mapValues { r => Greater(r, d) }

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def localGreaterRightAssociative(d: Double) =
    self.mapValues { r => Greater(d, r) }

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def >(d: Double) = localGreater(d)

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    *
    * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
    */
  def >>:(d: Double) = localGreaterRightAssociative(d)

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the rasters are greater than the next
    * raster, else 0.
    */
  def localGreater(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Greater.apply)

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the raster are greater than the next
    * raster, else 0.
    */
  def >(other: Seq[(K, Tile)]) = localGreater(other)
}
