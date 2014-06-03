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
import geotrellis.source._

/**
 * Determines if values are greater than other values. Sets to 1 if true, else 0.
 */
object Greater extends LocalTileComparatorOp {
  def compare(z1:Int,z2:Int):Boolean =
    if(z1 > z2) true else false

  def compare(z1:Double,z2:Double):Boolean =
    if(isNoData(z1)) { false }
    else {
      if(isNoData(z2)) { false }
      else { 
        if(z1 > z2) true
        else false
      }
    }
}

trait GreaterOpMethods[+Repr <: RasterSource] { self: Repr =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreater(i: Int): RasterSource = self.mapOp(Greater(_, i))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreaterRightAssociative(i: Int): RasterSource = self.mapOp(Greater(i, _))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def >(i:Int): RasterSource = localGreater(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   * 
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(i:Int): RasterSource = localGreaterRightAssociative(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreater(d: Double): RasterSource = self.mapOp(Greater(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreaterRightAssociative(d: Double): RasterSource = self.mapOp(Greater(d, _))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def >(d:Double): RasterSource = localGreater(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   * 
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(d:Double): RasterSource = localGreaterRightAssociative(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than the next raster, else 0.
   */
  def localGreater(rs:RasterSource): RasterSource = self.combineOp(rs)(Greater(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than the next raster, else 0.
   */
  def >(rs:RasterSource): RasterSource = localGreater(rs)
}

trait GreaterMethods { self: Tile =>
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreater(i: Int): Tile = Greater(self, i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreaterRightAssociative(i: Int): Tile = Greater(i, self)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def >(i:Int): Tile = localGreater(i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   * 
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(i:Int): Tile = localGreaterRightAssociative(i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreater(d: Double): Tile = Greater(self, d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreaterRightAssociative(d: Double): Tile = Greater(d, self)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def >(d:Double): Tile = localGreater(d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   * 
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(d:Double): Tile = localGreaterRightAssociative(d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than the next raster, else 0.
   */
  def localGreater(r:Tile): Tile = Greater(self,r)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the raster are greater than the next raster, else 0.
   */
  def >(r:Tile): Tile = localGreater(r)
}
