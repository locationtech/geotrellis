package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Equal
import geotrellis.raster.Tile

trait EqualRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the input
    * integer, else 0.
    */
  def localEqual(i: Int): RasterRDD[K, Tile] = 
    rasterRDD
      .mapPairs { case (t, r) =>
        (t, Equal(r, i))
      }
  
  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the input
    * double, else 0.
    */
  def localEqual(d: Double): RasterRDD[K, Tile] = 
    rasterRDD
      .mapPairs { case (t, r) =>
        (t, Equal(r, d))
      }

  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the provided
    * raster, else 0.
    */
  def localEqual(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] =
    rasterRDD.combineTiles(other) { case (t1, t2) => Equal(t1, t2) }
}
