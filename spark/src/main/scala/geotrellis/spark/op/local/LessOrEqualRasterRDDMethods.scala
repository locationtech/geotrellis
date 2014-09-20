package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.LessOrEqual
import geotrellis.spark.rdd.RasterRDD

trait LessOrEqualRasterRDDMethods extends RasterRDDMethods {
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def localLessOrEqual(i: Int): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, LessOrEqual(r, i)) }
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def localLessOrEqualRightAssociative(i: Int): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, LessOrEqual(i, r)) }
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
    * integer, else 0.
   */
  def <=(i: Int): RasterRDD = localLessOrEqual(i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def <=:(i: Int): RasterRDD = localLessOrEqualRightAssociative(i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def localLessOrEqual(d: Double): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, LessOrEqual(r, d)) }
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def localLessOrEqualRightAssociative(d: Double): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, LessOrEqual(d, r)) }
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def <=(d: Double): RasterRDD = localLessOrEqual(d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def <=:(d: Double): RasterRDD = localLessOrEqualRightAssociative(d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than or equal to the next raster, else 0.
   */
  def localLessOrEqual(other: RasterRDD): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, LessOrEqual(r1, r2))
  }
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than or equal to the next raster, else 0.
   */
  def <=(other: RasterRDD): RasterRDD = localLessOrEqual(other)
}
