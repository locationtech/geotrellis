package geotrellis.raster.op.focal

import geotrellis._

trait Cell[C <: Cell[C]] {
  def center(col:Int, row:Int) {}
  def clear():Unit
  def get():Int

  //def add(z:Int):Unit
  //def remove(z:Int):Unit
  //def add(col:Int, row:Int, r:Raster) { add(r.get(col, row)) }
  //def remove(col:Int, row:Int, r:Raster) { remove(r.get(col, row)) }
  def add(cc:C):Unit
  def remove(cc:C):Unit
  def add(col:Int, row:Int, r:Raster):Unit
  def remove(col:Int, row:Int, r:Raster):Unit
}

abstract class Context[A, C <: Cell[C]](val focalType:FocalType) {
  def store(col:Int, row:Int, cc:C):Unit
  def get():A
  def makeCell():C
}
