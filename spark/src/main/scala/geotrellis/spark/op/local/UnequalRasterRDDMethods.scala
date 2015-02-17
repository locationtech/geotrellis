package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Unequal

trait UnequalRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def localUnequal(i: Int): RasterRDD[K] = rasterRDD.mapPairs {
    case (t, r) => (t, Unequal(r, i))
  }
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def !==(i: Int): RasterRDD[K] = localUnequal(i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def !==:(i: Int): RasterRDD[K] = localUnequal(i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def localUnequal(d: Double): RasterRDD[K] = rasterRDD.mapPairs {
    case (t, r) => (t, Unequal(r, d))
  }
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def !==(d: Double): RasterRDD[K] = localUnequal(d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def !==:(d: Double): RasterRDD[K] = localUnequal(d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are not equal, else 0.
   */
  def localUnequal(other: RasterRDD[K]): RasterRDD[K] = rasterRDD.combineTiles(other) {
    case (t1, t2) => Unequal(t1, t2)
  }
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the raster are not equal, else 0.
   */
  def !==(other: RasterRDD[K]): RasterRDD[K] = localUnequal(other)
}
