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

import geotrellis._
import geotrellis.raster._

trait LocalMethods extends AddMethods
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
                      with PowMethods { self: Tile =>

  /**
   * Generate a raster with the values from the first raster, but only include
   * cells in which the corresponding cell in the second raster *are not* set to the 
   * "readMask" value. 
   *
   * For example, if *all* cells in the second raster are set to the readMask value,
   * the output raster will be empty -- all values set to NODATA.
   */
  def localMask(r:Tile, readMask:Int, writeMask:Int): Tile =
    Mask(self, r, readMask, writeMask)

  /**
    * Generate a raster with the values from the first raster, but only include
    * cells in which the corresponding cell in the second raster is set to the 
    * "readMask" value. 
    *
    * For example, if *all* cells in the second raster are set to the readMask value,
    * the output raster will be identical to the first raster.
    */
  def localInverseMask(r:Tile, readMask:Int, writeMask:Int): Tile =
    InverseMask(self, r, readMask, writeMask)

  /** Maps an integer typed Tile to 1 if the cell value is not NODATA, otherwise 0. */
  def localDefined(): Tile =
    Defined(self)

  /** Maps an integer typed Tile to 1 if the cell value is NODATA, otherwise 0. */
  def localUndefined(): Tile =
    Undefined(self)

  /** Take the square root each value in a raster. */
  def localSqrt(): Tile =
    Sqrt(self)

  /** Round the values of a Tile. */
  def localRound(): Tile =
    Round(self)

  /** Computes the Log of Tile values. */
  def localLog(): Tile =
    Log(self)

  /** Takes the Flooring of each raster cell value. */
  def localFloor(): Tile =
    Floor(self)

  /** Takes the Ceiling of each raster cell value. */
  def localCeil(): Tile =
    Ceil(self)

  /**
    * Negate (multiply by -1) each value in a raster.
    */
  def localNegate(): Tile =
    Negate(self)

  /** Negate (multiply by -1) each value in a raster. */
  def unary_-(): Tile = localNegate()

  /**
    * Bitwise negation of Tile.
    * @note               NotRaster does not currently support Double raster data.
    *                     If you use a Tile with a Double CellType (TypeFloat,TypeDouble)
    *                     the data values will be rounded to integers.
    */
  def localNot(): Tile =
    Not(self)

  /** Takes the Absolute value of each raster cell value. */
  def localAbs(): Tile =
  	Abs(self)

  /**
    * Takes the arc cos of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAcos(): Tile =
    Acos(self)

  /**
    * Takes the arc sine of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAsin(): Tile =
    Asin(self)

  /** Takes the Arc Tangent2
   *  This raster holds the y-values, and the parameter
   *  holds the x values. The arctan is calculated from y/x.
   *  @info               A double raster is always returned.
   */
   def localAtan2(r:Tile): Tile =
    Atan2(self, r)

  /**
    * Takes the arc tan of each raster cell value.
    * @info               Always return a double valued raster.
    */
  def localAtan(): Tile =
    Atan(self)

  /** Takes the Cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localCos(): Tile =
    Cos(self)

  /** Takes the hyperboic cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localCosh(): Tile =
    Cosh(self)

  /**
    * Takes the sine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localSin(): Tile = 
    Sin(self)

  /**
   * Takes the hyperbolic sine of each raster cell value.
   * @info               Always returns a double raster.
   */
  def localSinh(): Tile =
    Sinh(self)

  /** Takes the Tangent of each raster cell value.
   * @info               Always returns a double raster.
   */
  def localTan(): Tile =
    Tan(self)

  /** Takes the hyperboic cosine of each raster cell value.
    * @info               Always returns a double raster.
    */
  def localTanh(): Tile =
    Tanh(self)

 /** Gives the count of unique values at each location in a set of Tiles.*/
  def localVariety(rs:Seq[Tile]):Tile =
    Variety(self +: rs)

 /** Gives the count of unique values at each location in a set of Tiles.*/
  def localVariety(r:Tile*)(implicit d:DI):Tile =
    localVariety(r)

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(rs: Seq[Tile]): Tile =
    Mean(self +: rs)

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(r:Tile*)(implicit d:DI): Tile =
    localMean(r)
}
