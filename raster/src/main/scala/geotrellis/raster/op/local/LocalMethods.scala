/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.op.local

import geotrellis.raster._

trait LocalMethods extends TileMethods
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
                      with MajorityMethods
                      with MinorityMethods
                      with PowMethods {

  /**
   * Generate a raster with the values from the first raster, but only include
   * cells in which the corresponding cell in the second raster *are not* set to the
   * "readMask" value.
   *
   * For example, if *all* cells in the second raster are set to the readMask value,
   * the output raster will be empty -- all values set to NODATA.
   */
  def localMask(r: Tile, readMask: Int, writeMask: Int): Tile =
    Mask(tile, r, readMask, writeMask)

  /**
    * Generate a raster with the values from the first raster, but only include
    * cells in which the corresponding cell in the second raster is set to the
    * "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the readMask value,
    * the output raster will be identical to the first raster.
    */
  def localInverseMask(r: Tile, readMask: Int, writeMask: Int): Tile =
    InverseMask(tile, r, readMask, writeMask)

  /** Maps an integer typed Tile to 1 if the cell value is not NODATA, otherwise 0. */
  def localDefined(): Tile =
    Defined(tile)

  /** Maps an integer typed Tile to 1 if the cell value is NODATA, otherwise 0. */
  def localUndefined(): Tile =
    Undefined(tile)

  /** Take the square root each value in a raster. */
  def localSqrt(): Tile =
    Sqrt(tile)

  /** Round the values of a Tile. */
  def localRound(): Tile =
    Round(tile)

  /** Computes the Log of Tile values. */
  def localLog(): Tile =
    Log(tile)

  /** Computes the Log base 10 of Tile values. */
  def localLog10(): Tile =
    Log10(tile)

  /** Takes the Flooring of each raster cell value. */
  def localFloor(): Tile =
    Floor(tile)

  /** Takes the Ceiling of each raster cell value. */
  def localCeil(): Tile =
    Ceil(tile)

  /**
    * Negate (multiply by -1) each value in a raster.
    */
  def localNegate(): Tile =
    Negate(tile)

  /** Negate (multiply by -1) each value in a raster. */
  def unary_-(): Tile = localNegate()

  /**
    * Bitwise negation of Tile.
    * @note               NotRaster does not currently support Double raster data.
    *                     If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
    *                     the data values will be rounded to integers.
    */
  def localNot(): Tile =
    Not(tile)

  /** Takes the Absolute value of each raster cell value. */
  def localAbs(): Tile =
    Abs(tile)

  /**
    * Takes the arc cos of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAcos(): Tile =
    Acos(tile)

  /**
    * Takes the arc sine of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAsin(): Tile =
    Asin(tile)

  /** Takes the Arc Tangent2
   *  This raster holds the y - values, and the parameter
   *  holds the x values. The arctan is calculated from y / x.
   *  @info               A double raster is always returned.
   */
   def localAtan2(r: Tile): Tile =
    Atan2(tile, r)

  /**
    * Takes the arc tan of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAtan(): Tile =
    Atan(tile)

  /** Takes the Cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localCos(): Tile =
    Cos(tile)

  /** Takes the hyperboic cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localCosh(): Tile =
    Cosh(tile)

  /**
    * Takes the sine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localSin(): Tile =
    Sin(tile)

  /**
   * Takes the hyperbolic sine of each raster cell value.
   * @info               Always returns a double raster.
   */
  def localSinh(): Tile =
    Sinh(tile)

  /** Takes the Tangent of each raster cell value.
   * @info               Always returns a double raster.
   */
  def localTan(): Tile =
    Tan(tile)

  /** Takes the hyperboic cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localTanh(): Tile =
    Tanh(tile)
}
