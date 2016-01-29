package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.Greater
import org.apache.spark.rdd.RDD

trait GreaterTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def localGreater(i: Int) =
    self.mapValues { r => Greater(r, i) }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def localGreaterRightAssociative(i: Int) =
    self.mapValues { r => Greater(i, r) }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def >(i: Int) = localGreater(i)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    *
    * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
    */
  def >>:(i: Int) = localGreaterRightAssociative(i)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def localGreater(d: Double) =
    self.mapValues { r => Greater(r, d) }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def localGreaterRightAssociative(d: Double) =
    self.mapValues { r => Greater(d, r) }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def >(d: Double) = localGreater(d)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    *
    * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
    */
  def >>:(d: Double) = localGreaterRightAssociative(d)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell valued of the rasters are greater than the next
    * raster, else 0.
    */
  def localGreater(other: RDD[(K, Tile)]) =
    self.combineValues(other)(Greater.apply)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell valued of the raster are greater than the next
    * raster, else 0.
    */
  def >(other: RDD[(K, Tile)]) = localGreater(other)
}
