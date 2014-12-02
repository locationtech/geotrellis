package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.GreaterOrEqual

trait GreaterOrEqualRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def localGreaterOrEqual(i: Int): RasterRDD[K] = 
    rasterRDD
      .mapTiles { case (t, r) => 
        (t, GreaterOrEqual(r, i))
      }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def localGreaterOrEqualRightAssociative(i: Int): RasterRDD[K] = 
    rasterRDD
      .mapTiles { case (t, r) => 
        (t, GreaterOrEqual(i, r))
      }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def >=(i: Int): RasterRDD[K] = localGreaterOrEqual(i)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def >=:(i: Int): RasterRDD[K] = localGreaterOrEqualRightAssociative(i)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input double, else 0.
    */
  def localGreaterOrEqual(d: Double): RasterRDD[K] = 
    rasterRDD
      .mapTiles { case (t, r) => 
        (t, GreaterOrEqual(r, d))
      }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input double, else 0.
    */
  def localGreaterOrEqualRightAssociative(d: Double): RasterRDD[K] = 
    rasterRDD
      .mapTiles { case (t, r) => 
        (t, GreaterOrEqual(d, r))
      }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or
    * equal to the input double, else 0.
    */
  def >=(d: Double): RasterRDD[K] = localGreaterOrEqual(d)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or
    * equal to the input double, else 0.
    */
  def >=:(d: Double): RasterRDD[K] = localGreaterOrEqualRightAssociative(d)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell valued of the rasters are greater than or equal
    * to the next raster, else 0.
    */
  def localGreaterOrEqual(other: RasterRDD[K]): RasterRDD[K] =
    rasterRDD
      .combineTiles(other) { case ((t1, r1), (t2, r2)) => 
        (t1, GreaterOrEqual(r1, r2))
      }
  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell valued of the rasters are greater than or equal
    * to the next raster, else 0.
    */
  def >=(other: RasterRDD[K]): RasterRDD[K] = localGreaterOrEqual(other)
}
