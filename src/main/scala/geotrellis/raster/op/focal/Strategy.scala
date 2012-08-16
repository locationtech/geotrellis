package geotrellis.raster.op.focal

import geotrellis._

abstract class Strategy[A, C <: Cell[C]](val focalType:FocalType) {
  def store(col:Int, row:Int, cc:C):Unit
  def get():A
  def makeCell():C
}
