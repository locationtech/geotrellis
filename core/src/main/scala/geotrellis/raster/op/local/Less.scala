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
 * Determines if values are less than other values. Sets to 1 if true, else 0.
 */
object Less extends LocalTileComparatorOp {
  def compare(z1:Int,z2:Int):Boolean =
    if(z1 < z2) true else false

  def compare(z1:Double,z2:Double):Boolean =
    if(isNoData(z1)) { false }
    else {
      if(isNoData(z2)) { false }
      else { 
        if(z1 < z2) true
        else false
      }
    }
}

trait LessOpMethods[+Repr <: RasterSource] { self: Repr =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def localLess(i: Int): RasterSource = self.mapOp(Less(_, i))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def localLessRightAssociative(i: Int): RasterSource = self.mapOp(Less(i, _))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def <(i:Int): RasterSource = localLess(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   * 
   * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
   */
  def <<:(i:Int): RasterSource = localLessRightAssociative(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   */
  def localLess(d: Double): RasterSource = self.mapOp(Less(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   */
  def localLessRightAssociative(d: Double): RasterSource = self.mapOp(Less(d, _))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   */
  def <(d:Double): RasterSource = localLess(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   * 
   * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
   */
  def <<:(d:Double): RasterSource = localLessRightAssociative(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than the next raster, else 0.
   */
  def localLess(rs:RasterSource): RasterSource = self.combineOp(rs)(Less(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than the next raster, else 0.
   */
  def <(rs:RasterSource): RasterSource = localLess(rs)
}

trait LessMethods { self: Tile =>
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def localLess(i: Int): Tile = Less(self, i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def <(i:Int): Tile = localLess(i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def localLessRightAssociative(i: Int): Tile = Less(i, self)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   * 
   * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
   */
  def <<:(i:Int): Tile = localLessRightAssociative(i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   */
  def localLess(d: Double): Tile = Less(self, d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   */
  def localLessRightAssociative(d: Double): Tile = Less(self, d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   */
  def <(d:Double): Tile = localLess(d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   * 
   * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
   */
  def <<:(d:Double): Tile = localLessRightAssociative(d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than the next raster, else 0.
   */
  def localLess(r:Tile): Tile = Less(self,r)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than the next raster, else 0.
   */
  def <(r:Tile): Tile = localLess(r)
}
