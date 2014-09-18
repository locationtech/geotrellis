package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Greater
import geotrellis.spark.rdd.RasterRDD

trait GreaterRasterRDDMethods extends RasterRDDMethods {
  /**
   * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreater(i: Int): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Greater(r, i))
  }
  /**
   * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreaterRightAssociative(i: Int): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Greater(i, r))
  }
  /**
   * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def >(i: Int): RasterRDD = localGreater(i)
  /**
   * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   *
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(i: Int): RasterRDD = localGreaterRightAssociative(i)
  /**
   * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreater(d: Double): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Greater(d, r))
  }
  /**
   * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreaterRightAssociative(d: Double): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Greater(r, d))
  }
  /**
   * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def >(d: Double): RasterRDD = localGreater(d)
  /**
   * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   *
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(d: Double): RasterRDD = localGreaterRightAssociative(d)
  /**
   * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than the next raster, else 0.
   */
  def localGreater(other: RasterRDD): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, Greater(r1, r2))
  }
  /**
   * Returns a RasterRDD with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the raster are greater than the next raster, else 0.
   */
  def >(other: RasterRDD): RasterRDD = localGreater(other)
}
