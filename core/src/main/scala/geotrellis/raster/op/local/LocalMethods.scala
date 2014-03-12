/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster.op.local

import geotrellis._

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
                      with PowMethods { self: Raster =>

  /**
   * Generate a raster with the values from the first raster, but only include
   * cells in which the corresponding cell in the second raster *are not* set to the 
   * "readMask" value. 
   *
   * For example, if *all* cells in the second raster are set to the readMask value,
   * the output raster will be empty -- all values set to NODATA.
   */
  def localMask(r:Raster, readMask:Int, writeMask:Int): Raster =
    Mask(self, r, readMask, writeMask)

  /**
    * Generate a raster with the values from the first raster, but only include
    * cells in which the corresponding cell in the second raster is set to the 
    * "readMask" value. 
    *
    * For example, if *all* cells in the second raster are set to the readMask value,
    * the output raster will be identical to the first raster.
    */
  def localInverseMask(r:Raster, readMask:Int, writeMask:Int): Raster =
    InverseMask(self, r, readMask, writeMask)

  /** Maps an integer typed Raster to 1 if the cell value is not NODATA, otherwise 0. */
  def localDefined(): Raster =
    Defined(self)

  /** Maps an integer typed Raster to 1 if the cell value is NODATA, otherwise 0. */
  def localUndefined(): Raster =
    Undefined(self)

  /** Take the square root each value in a raster. */
  def localSqrt(): Raster =
    Sqrt(self)

  /** Round the values of a Raster. */
  def localRound(): Raster =
    Round(self)

  /** Computes the Log of Raster values. */
  def localLog(): Raster =
    Log(self)

  /** Takes the Flooring of each raster cell value. */
  def localFloor(): Raster =
    Floor(self)

  /** Takes the Ceiling of each raster cell value. */
  def localCeil(): Raster =
    Ceil(self)

  /**
    * Negate (multiply by -1) each value in a raster.
    */
  def localNegate(): Raster =
    Negate(self)

  /** Negate (multiply by -1) each value in a raster. */
  def unary_-(): Raster = localNegate()

  /**
    * Bitwise negation of Raster.
    * @note               NotRaster does not currently support Double raster data.
    *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
    *                     the data values will be rounded to integers.
    */
  def localNot(): Raster =
    Not(self)
}
