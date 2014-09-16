package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

trait LocalMethods extends MultiBandTileMethods
  with AddMethods
  with SubtractMethods
  with MultiplyMethods
  with DivideMethods
  with MinMethods
  with MaxMethods
  with AndMethods
  with OrMethods
  with XorMethods
  with ConditionalMethods
  with EqualMethods
  with UnequalMethods
  with GreaterOrEqualMethods
  with GreaterMethods
  with LessMethods
  with LessOrEqualMethods
  with PowMethods {

  val mTile: MultiBandTile

  /**
   * Generate a multiband raster with the values from the first multiband raster, but only include
   * cells in which the corresponding cell in the second multiband raster *are not* set to the
   * "readMask" value.
   *
   * For example, if *all* cells in the second multiband raster are set to the readMask value,
   * the output multiband raster will be empty -- all values set to NODATA.
   */
  def localMask(m: MultiBandTile, readMask: Int, writeMask: Int): MultiBandTile =
    Mask(mTile, m, readMask, writeMask)

  /**
   * Generate a multiband raster with the values from the first multiband raster, but only include
   * cells in which the corresponding cell in the second multiband raster is set to the
   * "readMask" value.
   *
   * For example, if *all* cells in the second multiband raster are set to the readMask value,
   * the output multiband raster will be identical to the first multiband raster.
   */
  def localInverseMask(m: MultiBandTile, readMask: Int, writeMask: Int): MultiBandTile =
    InverseMask(mTile, m, readMask, writeMask)

  /** Maps an integer typed MultiBandTile to 1 if the cell value is not NODATA, otherwise 0. */
  def localDefined(): MultiBandTile =
    Defined(mTile)

  /** Maps an integer typed MultiBandTile to 1 if the cell value is NODATA, otherwise 0. */
  def localUndefined(): MultiBandTile =
    Undefined(mTile)

  /** Take the square root each value in a multiband raster. */
  def localSqrt(): MultiBandTile =
    Sqrt(mTile)

  /** Round the values of a multiband raster. */
  def localRound(): MultiBandTile =
    Round(mTile)

  /** Computes the Log of MultiBandTile values. */
  def localLog(): MultiBandTile =
    Log(mTile)

  /** Takes the Flooring of each multiband raster cell value. */
  def localFloor(): MultiBandTile =
    Floor(mTile)

  /** Takes the Ceiling of each multiband raster cell value. */
  def localCeil(): MultiBandTile =
    Ceil(mTile)

  /**
   * Negate (multiply by -1) each value in a multiband raster.
   */
  def localNegate(): MultiBandTile =
    Negate(mTile)

  /** Negate (multiply by -1) each value in a multiband raster. */
  def unary_-(): MultiBandTile = localNegate()

  /**
   * Bitwise negation of MultiBandTile.
   * @note               NotRaster does not currently support Double multiband raster data.
   *                     If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
   *                     the data values will be rounded to integers.
   */
  def localNot(): MultiBandTile =
    Not(mTile)

  /** Takes the Absolute value of each multiband raster cell value. */
  def localAbs(): MultiBandTile =
    Abs(mTile)

  /**
   * Takes the arc cos of each multiband raster cell value.
   * @info               Always return a double valued multiband raster.
   */
  def localAcos(): MultiBandTile =
    Acos(mTile)

  /**
   * Takes the arc sine of each multiband raster cell value.
   * @info               Always return a double valued multiband raster.
   */
  def localAsin(): MultiBandTile =
    Asin(mTile)

  /**
   * Takes the Arc Tangent2
   *  This multiband raster holds the y - values, and the parameter
   *  holds the x values. The arctan is calculated from y / x.
   *  @info               A double multiband raster is always returned.
   */
  def localAtan2(m: MultiBandTile): MultiBandTile =
    Atan2(mTile, m)

  /**
   * Takes the arc tan of each multiband raster cell value.
   * @info               Always return a double valued multiband raster.
   */
  def localAtan(): MultiBandTile =
    Atan(mTile)

  /**
   * Takes the Cosine of each multiband raster cell value.
   * @info               Always returns a double multiband raster.
   */
  def localCos(): MultiBandTile =
    Cos(mTile)

  /**
   * Takes the hyperboic cosine of each raster multiband cell value.
   * @info               Always returns a double multiband raster.
   */
  def localCosh(): MultiBandTile =
    Cosh(mTile)

  /**
   * Takes the sine of each multiband raster cell value.
   * @info               Always returns a double multiband raster.
   */
  def localSin(): MultiBandTile =
    Sin(mTile)

  /**
   * Takes the hyperbolic sine of each multiband raster cell value.
   * @info               Always returns a double multiband raster.
   */
  def localSinh(): MultiBandTile =
    Sinh(mTile)

  /**
   * Takes the Tangent of each multiband raster cell value.
   * @info               Always returns a double multiband raster.
   */
  def localTan(): MultiBandTile =
    Tan(mTile)

  /**
   * Takes the hyperboic cosine of each multiband raster cell value.
   * @info               Always returns a double multiband raster.
   */
  def localTanh(): MultiBandTile =
    Tanh(mTile)

    /** 
     *  Take two bands Index and returne ndvi raster
     *  @info            two bands are red band and near infra band
     */
  def localNdvi(red: Int, nearInfra: Int): Tile =
    Ndvi(mTile, red, nearInfra)
}