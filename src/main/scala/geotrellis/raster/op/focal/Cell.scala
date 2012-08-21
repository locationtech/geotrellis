package geotrellis.raster.op.focal

import geotrellis._

trait Cell[C <: Cell[C]] {
  def center(col:Int, row:Int, r:Raster) {}
  def clear():Unit
  def add(cc:C):Unit
  def remove(cc:C):Unit
  def add(col:Int, row:Int, r:Raster):Unit
  def remove(col:Int, row:Int, r:Raster):Unit
}
