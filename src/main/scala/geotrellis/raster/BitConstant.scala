package geotrellis.raster

import geotrellis._

import scalaxy.loops._

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
  def mutable = BitArrayRasterData(Array.fill(length)(n), cols, rows)
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
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        forcedData.set(col,row,n)
      }
    }
    forcedData
  }
  
  def toArrayByte: Array[Byte] = throw new UnsupportedOperationException("BitConstant doesn't support this conversion")
}
