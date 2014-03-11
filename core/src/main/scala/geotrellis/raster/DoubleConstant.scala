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

package geotrellis.raster

import geotrellis._

import scalaxy.loops._

final case class DoubleConstant(n:Double, cols:Int, rows:Int) extends RasterData {
  def getType = TypeDouble
  def apply(i:Int) = n.asInstanceOf[Int]
  def applyDouble(i:Int) = n
  def length = cols * rows
  def alloc(cols:Int, rows:Int) = DoubleArrayRasterData.empty(cols, rows)
  def mutable = DoubleArrayRasterData(Array.ofDim[Double](length).fill(n), cols, rows)
  def copy = this

  override def combine(other:RasterData)(f:(Int,Int) => Int) = {
    val i = n.toInt
    other.map(z => f(i, z))
  }

  override def map(f:Int => Int) = IntConstant(f(n.toInt), cols, rows)
  override def foreach(f: Int => Unit) = foreachDouble(z => f(z.toInt))

  override def combineDouble(other:RasterData)(f:(Double,Double) => Double) = other.mapDouble(z => f(n, z))
  override def mapDouble(f:Double => Double) = DoubleConstant(f(n), cols, rows)
  override def foreachDouble(f: Double => Unit) = {
    var i = 0
    val len = length
    while (i < len) { f(n); i += 1 }
  }

  def force():RasterData = {
    val forcedData = RasterData.allocByType(getType,cols,rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        forcedData.setDouble(col,row,n)
      }
    }
    forcedData
  }
  
  def toArrayByte: Array[Byte] = Array(n.toByte)

  def warp(current:RasterExtent,target:RasterExtent):RasterData =
    DoubleConstant(n,target.cols,target.rows)
}
