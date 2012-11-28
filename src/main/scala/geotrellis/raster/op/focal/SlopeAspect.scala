package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.Raster

import scala.math._

import Angles._

trait SlopeAspectCalculator {
  def getSlopeAndAspect(cursor:Cursor[Int],zFactor:Double,cellWidth:Double,cellHeight:Double):(Double,Double) = {
    if(cursor.focusValue == NODATA) { return (Double.NaN,Double.NaN) }
    
    var east=0
    var west=0
    var north=0
    var south=0

    val fX = cursor.focusX
    val fY = cursor.focusY

    var y = cursor.ymin
    var x = 0
    while(y <= cursor.ymax) {
      x = cursor.xmin
      while(x <= cursor.xmax) {
        if(fX < x) {
          if(fY == y) { east += 2*cursor.get(x,y) }
          else { east += cursor.get(x,y) }
        }
        if(x < fX) { 
          if(fY == y) { west += 2*cursor.get(x,y) }
          else { west += cursor.get(x,y) }
        }
        if(fY < y) { 
          if(fX == x) { south += 2*cursor.get(x,y) }
          else { south += cursor.get(x,y) }
        }
        if(y < fY) { 
          if(fX == x) { north += 2*cursor.get(x,y) }
          else { north += cursor.get(x,y) }
        }
        x += 1
      }
      y += 1
    }
    // east - west
    val `dz/dx` = (east - west) / (8 * cellWidth)

    // south - north
    val `dz/dy` = (south - north) / (8 * cellHeight)

    val slope = atan(zFactor * sqrt(`dz/dx` * `dz/dx` + `dz/dy` * `dz/dy`))
    
    // Determine aspect based off of dz/dx and dz/dy
    var aspect = atan2(`dz/dy`, -`dz/dx`)
    
    return (slope,aspect)
  }

 def getSlopeAndAspectDouble(cursor:Cursor[Double],zFactor:Double,cellWidth:Double,cellHeight:Double):(Double,Double) = {
    if(cursor.focusValue == Double.NaN) { return (Double.NaN,Double.NaN) }
    
    var east=0.0
    var west=0.0
    var north=0.0
    var south=0.0

    val fX = cursor.focusX
    val fY = cursor.focusY

    var y = cursor.ymin
    var x = 0
    while(y <= cursor.ymax) {
      x = cursor.xmin
      while(x <= cursor.xmax) {
        if(fX < x) {
          if(fY == y) { east += 2*cursor.get(x,y) }
          else { east += cursor.get(x,y) }
        }
        if(x < fX) { 
          if(fY == y) { west += 2*cursor.get(x,y) }
          else { west += cursor.get(x,y) }
        }
        if(fY < y) { 
          if(fX == x) { south += 2*cursor.get(x,y) }
          else { south += cursor.get(x,y) }
        }
        if(y < fY) { 
          if(fX == x) { north += 2*cursor.get(x,y) }
          else { north += cursor.get(x,y) }
        }
        x += 1
      }
      y += 1
    }

    // east - west
    val `dz/dx` = (east - west) / (8 * cellWidth)

    // south - north
    val `dz/dy` = (south - north) / (8 * cellHeight)

    val slope = atan(zFactor * sqrt(`dz/dx` * `dz/dx` + `dz/dy` * `dz/dy`))
    
    // Determine aspect based off of dz/dx and dz/dy
    var aspect = atan2(`dz/dy`, -`dz/dx`)
    
    return (slope,aspect)
  }
}


case class Slope(r:Op[Raster], zFactorOp:Op[Double]) extends DoubleFocalOp1[Double,Raster](r,Square(1),zFactorOp) 
    with SlopeAspectCalculator {
  def createBuilder(r:Raster) = new DoubleRasterBuilder(r.rasterExtent)

  var zFactor = 0.0
  var cellWidth = 0.0
  var cellHeight = 0.0

  def init(r:Raster,z:Double) = {
    zFactor = z
    cellWidth = r.rasterExtent.cellwidth
    cellHeight = r.rasterExtent.cellheight
  }

  def calc(cursor:Cursor[Double]) = {
    val (slope,_) = getSlopeAndAspectDouble(cursor,zFactor,cellWidth,cellHeight)
    slope
  }
}

case class Aspect(r:Op[Raster], zFactorOp:Op[Double]) extends DoubleFocalOp1[Double,Raster](r,Square(1),zFactorOp) 
    with SlopeAspectCalculator {
  def createBuilder(r:Raster) = new DoubleRasterBuilder(r.rasterExtent)

  var zFactor = 0.0
  var cellWidth = 0.0
  var cellHeight = 0.0

  def init(r:Raster,z:Double) = {
    zFactor = z
    cellWidth = r.rasterExtent.cellwidth
    cellHeight = r.rasterExtent.cellheight
  }

  def calc(cursor:Cursor[Double]) = {
    val (_,aspect) = getSlopeAndAspectDouble(cursor,zFactor,cellWidth,cellHeight)
    aspect
  }
}


