package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Equal

trait EqualRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the input
    * integer, else 0.
    */
  def localEqual(i: Int): RasterRDD[K] = 
    rasterRDD.mapValues { r => Equal(r, i) }
  
  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the input
    * double, else 0.
    */
  def localEqual(d: Double): RasterRDD[K] = 
    rasterRDD.mapValues { r => Equal(r, d) }

  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the provided
    * raster, else 0.
    */
  def localEqual(other: RasterRDD[K]): RasterRDD[K] =
    rasterRDD.combineValues(other) { case (t1, t2) => Equal(t1, t2) }
}
