package geotrellis.spark.op.local

import geotrellis.raster.op.local._
import geotrellis.raster._
import geotrellis.spark._

trait LocalRasterRDDMethods[K] extends RasterRDDMethods[K]
    with AddRasterRDDMethods[K]
    with AndRasterRDDMethods[K]
    with IfCellRasterRDDMethods[K]
    with DivideRasterRDDMethods[K]
    with EqualRasterRDDMethods[K]
    with GreaterOrEqualRasterRDDMethods[K]
    with GreaterRasterRDDMethods[K]
    with LessOrEqualRasterRDDMethods[K]
    with LessRasterRDDMethods[K]
    with LocalMapRasterRDDMethods[K]
    with MajorityRasterRDDMethods[K]
    with MaxRasterRDDMethods[K]
    with MinRasterRDDMethods[K]
    with MinorityRasterRDDMethods[K]
    with MultiplyRasterRDDMethods[K]
    with OrRasterRDDMethods[K]
    with PowRasterRDDMethods[K]
    with SubtractRasterRDDMethods[K]
    with UnequalRasterRDDMethods[K]
    with XorRasterRDDMethods[K] {

  /**
    * Generate a raster with the values from the first raster, but only include
    * cells in which the corresponding cell in the second raster *are not* set to the
    * "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the readMask value,
    * the output raster will be empty -- all values set to NODATA.
    */
  def localMask(other: RasterRDD[K, Tile], readMask: Int, writeMask: Int): RasterRDD[K, Tile] =
    rasterRDD.combinePairs(other) {
      case ((t1, r1), (t2, r2)) =>
        (t1, Mask(r1, r2, readMask, writeMask))
    }

  /**
    * Generate a raster with the values from the first raster, but only include
    * cells in which the corresponding cell in the second raster is set to the
    * "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the readMask value,
    * the output raster will be identical to the first raster.
    */
  def localInverseMask(other: RasterRDD[K, Tile], readMask: Int, writeMask: Int): RasterRDD[K, Tile] =
    rasterRDD.combinePairs(other) {
      case ((t1, r1), (t2, r2)) =>
        (t1, InverseMask(r1, r2, readMask, writeMask))
    }

  /** Maps an integer typed Tile to 1 if the cell value is not NODATA, otherwise 0. */
  def localDefined(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Defined(r)) }

  /** Maps an integer typed Tile to 1 if the cell value is NODATA, otherwise 0. */
  def localUndefined(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Undefined(r)) }

  /** Take the square root each value in a raster. */
  def localSqrt(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Sqrt(r)) }

  /** Round the values of a Tile. */
  def localRound(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Round(r)) }

  /** Computes the Log of Tile values. */
  def localLog(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Log(r)) }

  /** Computes the Log base 10 of Tile values. */
  def localLog10(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Log10(r)) }

  /** Takes the Flooring of each raster cell value. */
  def localFloor(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Floor(r)) }

  /** Takes the Ceiling of each raster cell value. */
  def localCeil(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Ceil(r)) }

  /**
    * Negate (multiply by -1) each value in a raster.
    */
  def localNegate(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Negate(r)) }

  /** Negate (multiply by -1) each value in a raster. */
  def unary_-(): RasterRDD[K, Tile] = localNegate()

  /**
    * Bitwise negation of Tile.
    * @note               NotRaster does not currently support Double raster data.
    *                     If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
    *                     the data values will be rounded to integers.
    */
  def localNot(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Not(r)) }

  /** Takes the Absolute value of each raster cell value. */
  def localAbs(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Abs(r)) }

  /**
    * Takes the arc cos of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAcos(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Acos(r)) }

  /**
    * Takes the arc sine of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAsin(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Asin(r)) }

  /** Takes the Arc Tangent2
    *  This raster holds the y - values, and the parameter
    *  holds the x values. The arctan is calculated from y / x.
    *  @info               A double raster is always returned.
    */
  def localAtan2(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = rasterRDD.combinePairs(other) {
    case ((t1, r1), (t2, r2)) => (t1, Atan2(r1, r2))
  }

  /**
    * Takes the arc tan of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAtan(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Atan(r)) }

  /** Takes the Cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localCos(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Cos(r)) }

  /** Takes the hyperbolic cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localCosh(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Cosh(r)) }

  /**
    * Takes the sine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localSin(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Sin(r)) }

  /**
    * Takes the hyperbolic sine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localSinh(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Sinh(r)) }

  /** Takes the Tangent of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localTan(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Tan(r)) }

  /** Takes the hyperboic cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localTanh(): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Tanh(r)) }

  def minMax: (Int, Int) =
    rasterRDD
      .map(_.tile.findMinMax)
      .reduce { (t1, t2) =>
        val (min1, max1) = t1
        val (min2, max2) = t2
        val min =
          if(isNoData(min1)) min2
          else {
            if(isNoData(min2)) min1
            else math.min(min1, min2)
          }
        val max =
          if(isNoData(max1)) max2
          else {
            if(isNoData(max2)) max1
            else math.max(max1, max2)
          }
        (min, max)
      }

  def minMaxDouble: (Double, Double) =
    rasterRDD
      .map(_.tile.findMinMaxDouble)
      .reduce { (t1, t2) =>
        val (min1, max1) = t1
        val (min2, max2) = t2
        val min =
          if(isNoData(min1)) min2
          else {
            if(isNoData(min2)) min1
            else math.min(min1, min2)
          }
        val max =
          if(isNoData(max1)) max2
          else {
            if(isNoData(max2)) max1
            else math.max(max1, max2)
          }
        (min, max)
      }
}
