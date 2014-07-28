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

package geotrellis.raster.stats

case class ConstantHistogram(value:Int,size:Int) extends Histogram {
  def copy = ConstantHistogram(value,size)

  def foreachValue(f:Int=>Unit) = f(value)
  def getItemCount(item:Int):Int = if(item == value) size else 0
  def getMaxValue():Int = value
  def getMinValue():Int = value
  def getTotalCount():Int = size
  def getValues():Array[Int] = Array(value)
  def rawValues():Array[Int] = Array(value)
  def getQuantileBreaks(num:Int):Array[Int] = Array(value)
  def mutable():MutableHistogram = {
    val fmh = FastMapHistogram(size)
    fmh.setItem(value,size)
    fmh
  }
}

