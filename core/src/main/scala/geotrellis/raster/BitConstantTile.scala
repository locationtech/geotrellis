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

package geotrellis.raster

import geotrellis._
import geotrellis.feature.Extent

import scalaxy.loops._

object BitConstantTile {
  def apply(v: Boolean, cols: Int, rows: Int): BitConstant =
    if(v) BitConstantTile(1.toByte,cols, rows) else BitConstantTile(0.toByte, cols, rows)
}

final case class BitConstantTile(n: Byte, cols: Int, rows: Int) extends ArrayTile {
  def getType = TypeBit

  private val iVal: Int = n.toInt
  private val dVal: Double = n.toDouble

  def apply(i: Int) = iVal
  def applyDouble(i: Int) = dVal
  def alloc(cols: Int, rows: Int) = BitArrayTile.empty(cols, rows)
  def mutable = BitArrayTile(Array.ofDim[Byte](length).fill(n), cols, rows)
  def copy = this

  override def combine(other: ArrayTile)(f: (Int, Int) => Int) = other.map(z => f(iVal, z))
  override def map(f: Int => Int) = BitConstantTile(f(iVal).toByte, cols, rows)

  override def foreach(f: Int => Unit) = {
    var i = 0
    val len = length
    while (i < len) { f(iVal); i += 1 }
  }

  override def combineDouble(other: ArrayTile)(f: (Double, Double) => Double) = 
    other.mapDouble(z => f(dVal, z))
  override def mapDouble(f: Double => Double) = DoubleConstantTile(f(dVal), cols, rows)
  override def foreachDouble(f: Double => Unit) = {
    var i = 0
    val len = length
    while (i < len) { f(dVal); i += 1 }
  }

  def force(): ArrayTile = {
    val forcedData = ArrayTile.allocByType(getType, cols, rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        forcedData.set(col, row, n)
      }
    }
    forcedData
  }
  
  def toArrayByte: Array[Byte] = throw new UnsupportedOperationException("BitConstant doesn't support this conversion")

  def warp(current: Extent, target: RasterExtent): ArrayTile =
    BitConstantTile(n, target.cols, target.rows)
}
