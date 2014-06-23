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

import spire.syntax.cfor._

object BitConstant {
  def apply(v:Boolean, cols:Int, rows:Int):BitConstant =
    if(v) BitConstant(1.toByte,cols,rows) else BitConstant(0.toByte,cols,rows)
}

final case class BitConstant(n:Byte, cols:Int, rows:Int) extends RasterData {
  def getType = TypeBit

  private val iVal:Int = n.toInt
  private val dVal:Double = n.toDouble

  def apply(i:Int) = iVal
  def applyDouble(i:Int) = dVal
  def length = cols * rows
  def alloc(cols:Int, rows:Int) = BitArrayRasterData.empty(cols, rows)
  def mutable = BitArrayRasterData(Array.ofDim[Byte](length).fill(n), cols, rows)
  def copy = this

  override def combine(other:RasterData)(f:(Int,Int) => Int) = other.map(z => f(iVal, z))
  override def map(f:Int => Int) = BitConstant(f(iVal).toByte, cols, rows)

  override def foreach(f: Int => Unit) = {
    var i = 0
    val len = length
    while (i < len) { f(iVal); i += 1 }
  }

  override def combineDouble(other:RasterData)(f:(Double,Double) => Double) = 
    other.mapDouble(z => f(dVal, z))
  override def mapDouble(f:Double => Double) = DoubleConstant(f(dVal), cols, rows)
  override def foreachDouble(f: Double => Unit) = {
    var i = 0
    val len = length
    while (i < len) { f(dVal); i += 1 }
  }

  def force():RasterData = {
    val forcedData = RasterData.allocByType(getType,cols,rows)
    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        forcedData.set(col,row,n)
      }
    }
    forcedData
  }
  
  def toArrayByte: Array[Byte] = throw new UnsupportedOperationException("BitConstant doesn't support this conversion")

  def warp(current:RasterExtent,target:RasterExtent):RasterData =
    BitConstant(n,target.cols,target.rows)
}
