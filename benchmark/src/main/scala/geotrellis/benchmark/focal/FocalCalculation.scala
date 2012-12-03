package geotrellis.benchmark.oldfocal

import geotrellis._
import geotrellis.raster._
import geotrellis.raster.op.focal._

trait FocalCalculation[@specialized(Int,Double)D] {
  def center(col:Int, row:Int, r:Raster) {}
  def clear():Unit
  def add(col:Int, row:Int, r:Raster):Unit
  def remove(col:Int, row:Int, r:Raster):Unit
  def getResult():D
}
