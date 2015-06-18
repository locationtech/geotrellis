package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Greater
import geotrellis.raster.Tile

trait GreaterRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def localGreater(i: Int): RasterRDD[K, Tile] = 
    rasterRDD
      .mapPairs { case (t, r) =>
        (t, Greater(r, i))
      }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def localGreaterRightAssociative(i: Int): RasterRDD[K, Tile] = 
    rasterRDD
      .mapPairs { case (t, r) =>
        (t, Greater(i, r))
      }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def >(i: Int): RasterRDD[K, Tile] = localGreater(i)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    *
    * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
    */
  def >>:(i: Int): RasterRDD[K, Tile] = localGreaterRightAssociative(i)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def localGreater(d: Double): RasterRDD[K, Tile] = 
    rasterRDD
      .mapPairs { case (t, r) =>
        (t, Greater(r, d))
      }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def localGreaterRightAssociative(d: Double): RasterRDD[K, Tile] = 
    rasterRDD
      .mapPairs { case (t, r) =>
        (t, Greater(d, r))
      }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def >(d: Double): RasterRDD[K, Tile] = localGreater(d)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    *
    * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
    */
  def >>:(d: Double): RasterRDD[K, Tile] = localGreaterRightAssociative(d)

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell valued of the rasters are greater than the next
    * raster, else 0.
    */
  def localGreater(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] =
    rasterRDD.combineTiles(other) { case (t1, t2) => Greater(t1, t2) }

  /**
    * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
    * the corresponding cell valued of the raster are greater than the next
    * raster, else 0.
    */
  def >(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = localGreater(other)
}
