package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Less
import geotrellis.raster.Tile

trait LessRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    */
  def localLess(i: Int): RasterRDD[K, Tile] = 
    rasterRDD.mapPairs { case (t, r) =>
      (t, Less(r, i))
    }

  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    */
  def <(i: Int): RasterRDD[K, Tile] = localLess(i)

  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    */
  def localLessRightAssociative(i: Int): RasterRDD[K, Tile] = 
    rasterRDD.mapPairs { case (t, r) =>
      (t, Less(i, r))
    }

  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    *
    * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
    */
  def <<:(i: Int): RasterRDD[K, Tile] = localLessRightAssociative(i)

  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    */
  def localLess(d: Double): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Less(r, d))
  }
  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    */
  def localLessRightAssociative(d: Double): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Less(d, r))
  }
  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    */
  def <(d: Double): RasterRDD[K, Tile] = localLess(d)
  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    *
    * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
    */
  def <<:(d: Double): RasterRDD[K, Tile] = localLessRightAssociative(d)
  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell valued of the rasters are less than the next
    * raster, else 0.
    */
  def localLess(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = rasterRDD.combineTiles(other) {
    case (t1, t2) => Less(t1, t2)
  }
  /**
    * Returns a Tile with data of TypeBit, where cell values equal 1 if
    * the corresponding cell valued of the rasters are less than the next
    * raster, else 0.
    */
  def <(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = localLess(other)
}
