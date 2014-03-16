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
 * Determines if values are less than or equal to other values. Sets to 1 if true, else 0.
 */
object LessOrEqual extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) =
    if(z1 <= z2) 1 else 0

  def combine(z1:Double,z2:Double):Double =
    if(isNoData(z1)) { 0 }
    else {
      if(isNoData(z2)) { 0 }
      else { 
        if(z1 <= z2) 1 
        else 0 
      }
    }
}

trait LessOrEqualOpMethods[+Repr <: RasterSource] { self: Repr =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def localLessOrEqual(i: Int) = self.mapOp(LessOrEqual(_, i))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def localLessOrEqualRightAssociative(i: Int) = self.mapOp(LessOrEqual(i, _))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
    * integer, else 0.
   */
  def <=(i:Int) = localLessOrEqual(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def <=:(i:Int) = localLessOrEqualRightAssociative(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def localLessOrEqual(d: Double) = self.mapOp(LessOrEqual(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def localLessOrEqualRightAssociative(d: Double) = self.mapOp(LessOrEqual(d, _))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def <=(d:Double) = localLessOrEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def <=:(d:Double) = localLessOrEqualRightAssociative(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than or equal to the next raster, else 0.
   */
  def localLessOrEqual(rs:RasterSource) = self.combineOp(rs)(LessOrEqual(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than or equal to the next raster, else 0.
   */
  def <=(rs:RasterSource) = localLessOrEqual(rs)
}

trait LessOrEqualMethods { self: Raster =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def localLessOrEqual(i: Int) = LessOrEqual(self, i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def localLessOrEqualRightAssociative(i: Int) = LessOrEqual(i, self)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
    * integer, else 0.
   */
  def <=(i:Int) = localLessOrEqual(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * integer, else 0.
   */
  def <=:(i:Int) = localLessOrEqualRightAssociative(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def localLessOrEqual(d: Double) = LessOrEqual(self, d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def localLessOrEqualRightAssociative(d: Double) = LessOrEqual(d, self)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def <=(d:Double) = localLessOrEqual(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than or equal to the input
   * double, else 0.
   */
  def <=:(d:Double) = localLessOrEqualRightAssociative(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than or equal to the next raster, else 0.
   */
  def localLessOrEqual(r:Raster) = LessOrEqual(self,r)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than or equal to the next raster, else 0.
   */
  def <=(r:Raster) = localLessOrEqual(r)
}
