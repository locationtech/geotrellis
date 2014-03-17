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
import geotrellis.source._

/**
 * Maps all cells matching `cond` to Int `trueValue`.
 */
object IfCell extends Serializable {
  /**
   * Maps all cells matching `cond` to Int `trueValue`.
   */
  def apply(r:Raster, cond:Int => Boolean, trueValue:Int): Raster =
    r.dualMap {z: Int => if (cond(z)) trueValue else z }
              {z:Double => if (cond(d2i(z))) i2d(trueValue) else z}

  /**
   * Maps all cells matching `cond` to Double `trueValue`.
   */
  def apply(r:Raster, cond:Double => Boolean, trueValue:Double): Raster =
    r.dualMap {z: Int => if (cond(i2d(z))) d2i(trueValue) else z}
              {z: Double => if (cond(z)) trueValue else z}

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false.  */
  def apply(r:Raster, cond:Int => Boolean, trueValue:Int, falseValue: Int): Raster =
    r.dualMap {z: Int => if (cond(z)) trueValue else falseValue}
              {z: Double => if (cond(d2i(z))) i2d(trueValue) else i2d(falseValue)}

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false for Double values.  */
  def apply(r:Raster, cond:Double => Boolean, trueValue:Double, falseValue: Double): Raster =
    r.dualMap {z: Int => if (cond(i2d(z))) d2i(trueValue) else d2i(falseValue) }
              {z: Double => if (cond(z)) trueValue else falseValue }

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def apply(r1:Raster, r2:Raster, cond:(Int, Int) => Boolean, trueValue:Int): Raster =
    r1.dualCombine(r2) { (z1,z2) => if (cond(z1, z2)) trueValue else z1 }
                       { (z1,z2) => if (cond(d2i(z1), d2i(z2))) i2d(trueValue) else z1 }

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def apply(r1:Raster, r2:Raster, cond:(Double, Double) => Boolean, trueValue:Double): Raster =
    r1.dualCombine(r2) { (z1,z2) => if (cond(i2d(z1), i2d(z2))) d2i(trueValue) else z1 }
                       { (z1,z2) => if (cond(z1, z2)) trueValue else z1 }

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on 
   * the corresponding values in each of the two input rasters.*/
  def apply(r1:Raster, r2:Raster, cond:(Int, Int) => Boolean, trueValue:Int, falseValue:Int): Raster =
    r1.dualCombine(r2) { (z1,z2) => if(cond(z1,z2)) trueValue else falseValue }
                       { (z1,z2) => if (cond(d2i(z1), d2i(z2))) i2d(trueValue) else i2d(falseValue) }

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on 
   * the corresponding values in each of the two input rasters.*/
  def apply(r1:Raster, r2:Raster, cond:(Double, Double) => Boolean, trueValue:Double, falseValue:Double): Raster =
    r1.dualCombine(r2) { (z1,z2) => if(cond(i2d(z1),i2d(z2))) d2i(trueValue) else d2i(falseValue) }
                       { (z1,z2) => if (cond(z1, z2)) trueValue else falseValue }
}


trait ConditionalOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Maps all cells matching `cond` to Int `trueValue`.*/
  def localIf(cond:Int => Boolean,trueValue:Int): RasterSource =
    map(IfCell(_,cond,trueValue), "IfCell")

  /** Maps all cells matching `cond` to Double `trueValue`.*/
  def localIf(cond:Double => Boolean,trueValue:Double): RasterSource =
    map(IfCell(_,cond,trueValue), "IfCell")

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false.  */
  def localIf(cond:Int => Boolean,trueValue:Int,falseValue:Int): RasterSource =
    map(IfCell(_,cond,trueValue,falseValue), "IfCell")

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false for Double values.  */
  def localIf(cond:Double => Boolean,trueValue:Double,falseValue:Double): RasterSource =
    map(IfCell(_,cond,trueValue,falseValue), "IfCell")

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def localIf(rs:RasterSource,cond:(Int,Int)=>Boolean,trueValue:Int): RasterSource =
    combine(rs, "BinaryIfCell")(IfCell(_,_,cond,trueValue))

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def localIf(rs:RasterSource,cond:(Double,Double)=>Boolean,trueValue:Double): RasterSource =
    combine(rs, "BinaryIfCell")(IfCell(_,_,cond,trueValue))

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on
   * the corresponding values in each of the two input rasters.*/
  def localIf(rs:RasterSource,cond:(Int,Int)=>Boolean,trueValue:Int,falseValue:Int): RasterSource =
    combine(rs, "BinaryIfElseCell")(IfCell(_,_,cond,trueValue,falseValue))

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on
   * the corresponding values in each of the two input rasters.*/
  def localIf(rs:RasterSource,cond:(Double,Double)=>Boolean,trueValue:Double,falseValue:Double): RasterSource =
    combine(rs, "BinaryIfElseCell")(IfCell(_,_,cond,trueValue,falseValue))
}


trait ConditionalMethods {self: Raster =>
  def localIf(cond:Int => Boolean,trueValue:Int): Raster =
    IfCell(self,cond,trueValue)
  def localIf(cond:Double => Boolean,trueValue:Double): Raster =
    IfCell(self,cond,trueValue)
  def localIf(cond:Int => Boolean,trueValue:Int,falseValue:Int): Raster =
    IfCell(self,cond,trueValue,falseValue)
  def localIf(cond:Double => Boolean,trueValue:Double,falseValue:Double): Raster =
    IfCell(self,cond,trueValue,falseValue)
  def localIf(r:Raster,cond:(Int,Int)=>Boolean,trueValue:Int): Raster =
    IfCell(self,r,cond,trueValue)
  def localIf(r:Raster,cond:(Double,Double)=>Boolean,trueValue:Double): Raster =
    IfCell(self,r,cond,trueValue)
  def localIf(r:Raster,cond:(Int,Int)=>Boolean,trueValue:Int,falseValue:Int): Raster =
    IfCell(self,r,cond,trueValue,falseValue)
  def localIf(r:Raster,cond:(Double,Double)=>Boolean,trueValue:Double,falseValue:Double): Raster =
    IfCell(self,r,cond,trueValue,falseValue)
}
