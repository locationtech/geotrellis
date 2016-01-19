package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.GreaterOrEqual

trait GreaterOrEqualTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def localGreaterOrEqual(i: Int) =
    self.mapValues { r => GreaterOrEqual(r, i) }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def localGreaterOrEqualRightAssociative(i: Int) =
    self.mapValues { r => GreaterOrEqual(i, r) }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def >=(i: Int) = localGreaterOrEqual(i)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def >=:(i: Int) = localGreaterOrEqualRightAssociative(i)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input double, else 0.
    */
  def localGreaterOrEqual(d: Double) =
    self.mapValues { r => GreaterOrEqual(r, d) }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input double, else 0.
    */
  def localGreaterOrEqualRightAssociative(d: Double) =
    self.mapValues { r => GreaterOrEqual(d, r) }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or
    * equal to the input double, else 0.
    */
  def >=(d: Double) = localGreaterOrEqual(d)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or
    * equal to the input double, else 0.
    */
  def >=:(d: Double) = localGreaterOrEqualRightAssociative(d)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell valued of the rasters are greater than or equal
    * to the next raster, else 0.
    */
  def localGreaterOrEqual(other: Self) =
    self.combineValues(other)(GreaterOrEqual.apply)
  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell valued of the rasters are greater than or equal
    * to the next raster, else 0.
    */
  def >=(other: Self) = localGreaterOrEqual(other)
}
