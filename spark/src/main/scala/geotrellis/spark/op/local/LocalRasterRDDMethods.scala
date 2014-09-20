package geotrellis.spark.op.local

import geotrellis.raster.op.local._
import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

trait LocalRasterRDDMethods extends RasterRDDMethods
    with AddRasterRDDMethods
    with AndRasterRDDMethods
    with ConditionalRasterRDDMethods
    with DivideRasterRDDMethods
    with EqualRasterRDDMethods
    with GreaterOrEqualRasterRDDMethods
    with GreaterRasterRDDMethods
    with LessOrEqualRasterRDDMethods
    with LessRasterRDDMethods
    with MajorityRasterRDDMethods
    with MaxRasterRDDMethods
    with MinRasterRDDMethods
    with MinorityRasterRDDMethods
    with MultiplyRasterRDDMethods
    with OrRasterRDDMethods
    with PowRasterRDDMethods
    with SubtractRasterRDDMethods
    with UnequalRasterRDDMethods
    with XorRasterRDDMethods {

  /**
    * Generate a raster with the values from the first raster, but only include
    * cells in which the corresponding cell in the second raster *are not* set to the
    * "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the readMask value,
    * the output raster will be empty -- all values set to NODATA.
    */
  def localMask(other: RasterRDD, readMask: Int, writeMask: Int): RasterRDD =
    rasterRDD.combineTiles(other) {
      case (TmsTile(t1, r1), TmsTile(t2, r2)) =>
        TmsTile(t1, Mask(r1, r2, readMask, writeMask))
    }

  /**
    * Generate a raster with the values from the first raster, but only include
    * cells in which the corresponding cell in the second raster is set to the
    * "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the readMask value,
    * the output raster will be identical to the first raster.
    */
  def localInverseMask(other: RasterRDD, readMask: Int, writeMask: Int): RasterRDD =
    rasterRDD.combineTiles(other) {
      case (TmsTile(t1, r1), TmsTile(t2, r2)) =>
        TmsTile(t1, InverseMask(r1, r2, readMask, writeMask))
    }

  /** Maps an integer typed Tile to 1 if the cell value is not NODATA, otherwise 0. */
  def localDefined(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Defined(r)) }

  /** Maps an integer typed Tile to 1 if the cell value is NODATA, otherwise 0. */
  def localUndefined(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Undefined(r)) }

  /** Take the square root each value in a raster. */
  def localSqrt(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Sqrt(r)) }

  /** Round the values of a Tile. */
  def localRound(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Round(r)) }

  /** Computes the Log of Tile values. */
  def localLog(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Log(r)) }

  /** Takes the Flooring of each raster cell value. */
  def localFloor(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Floor(r)) }

  /** Takes the Ceiling of each raster cell value. */
  def localCeil(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Ceil(r)) }

  /**
    * Negate (multiply by -1) each value in a raster.
    */
  def localNegate(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Negate(r)) }

  /** Negate (multiply by -1) each value in a raster. */
  def unary_-(): RasterRDD = localNegate()

  /**
    * Bitwise negation of Tile.
    * @note               NotRaster does not currently support Double raster data.
    *                     If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
    *                     the data values will be rounded to integers.
    */
  def localNot(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Not(r)) }

  /** Takes the Absolute value of each raster cell value. */
  def localAbs(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Abs(r)) }

  /**
    * Takes the arc cos of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAcos(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Acos(r)) }

  /**
    * Takes the arc sine of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAsin(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Asin(r)) }

  /** Takes the Arc Tangent2
    *  This raster holds the y - values, and the parameter
    *  holds the x values. The arctan is calculated from y / x.
    *  @info               A double raster is always returned.
    */
  def localAtan2(other: RasterRDD): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, Atan2(r1, r2))
  }

  /**
    * Takes the arc tan of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAtan(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Atan(r)) }

  /** Takes the Cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localCos(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Cos(r)) }

  /** Takes the hyperboic cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localCosh(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Cosh(r)) }

  /**
    * Takes the sine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localSin(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Sin(r)) }

  /**
    * Takes the hyperbolic sine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localSinh(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Sinh(r)) }

  /** Takes the Tangent of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localTan(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Tan(r)) }

  /** Takes the hyperboic cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localTanh(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Tanh(r)) }
}
