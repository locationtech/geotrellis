package geotrellis.raster.op.focal

import geotrellis._

trait Cell {
  def clear():Unit
  def get():Int
  def copy():Cell

  def add(z:Int):Unit
  def remove(z:Int):Unit

  def center(col:Int, row:Int) {}
  def add(col:Int, row:Int, r:Raster) { add(r.get(col, row)) }
  def remove(col:Int, row:Int, r:Raster) { remove(r.get(col, row)) }
}

abstract class Context[A](val focalType:FocalType) {
  def store(col:Int, row:Int, z:Int):Unit
  def get():A
}
