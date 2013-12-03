package geotrellis.statistics

import geotrellis._

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

