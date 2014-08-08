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

/**
 * Determines if values are equal. Sets to 1 if true, else 0.
 */
object Equal extends LocalTileComparatorOp {
  def compare(z1:Int,z2:Int):Boolean =
    if(z1 == z2) true else false

  def compare(z1:Double,z2:Double):Boolean =
    if(isNoData(z1)) { if(isNoData(z2)) true else false }
    else {
      if(isNoData(z2)) { false }
      else { 
        if(z1 == z2) true
        else false
      }
    }
}

trait EqualMethods extends TileMethods {
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def localEqual(i: Int): Tile = Equal(tile, i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def localEqual(d: Double): Tile = Equal(tile, d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def localEqual(r:Tile): Tile = Equal(tile,r)
}
