package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.process._

import scala.math._

import Angles._

class SurfacePoint() {
  var `dz/dx` = Double.NaN
  var `dz/dy` = Double.NaN

  def aspect() = atan2(`dz/dy`, -`dz/dx`)
  def slope(zFactor:Double):Double = atan(zFactor * sqrt(`dz/dx` * `dz/dx` + `dz/dy` * `dz/dy`))
  def slope():Double = slope(1.0)

  // Trig functions for slope and aspect.
  // Use these if you want to get the sine or cosine of the aspect or slope,
  // since they are a lot more performant than calling scala.math.sin or 
  // scala.math.cos
  def cosSlope = {
    val denom = sqrt(`dz/dx` * `dz/dx` + `dz/dy` * `dz/dy` + 1)
    if(denom == 0) Double.NaN else {
      1/denom
    }
  }
  def sinSlope = {
    val denom = sqrt(`dz/dx` * `dz/dx` + `dz/dy` * `dz/dy` + 1)
    if(denom == 0) Double.NaN else {
      sqrt(`dz/dx` * `dz/dx` + `dz/dy` * `dz/dy`) / denom
    }
  }
  def cosAspect = {
    if(`dz/dx` == 0) { if(`dz/dy` == 0) -1 else 0 } else {
      if(`dz/dy` == 0) {
        if(`dz/dx` < 0) 1 else -1
      } else {
        -`dz/dx` / sqrt(`dz/dy`*`dz/dy` + `dz/dx`*`dz/dx`)
      }
    }
  }
  def sinAspect = {
    if(`dz/dy` == 0) 0 else {
      if(`dz/dx` == 0) {
        if(`dz/dy` < 0) -1 else if(`dz/dy` > 0) 1 else 0
      } else {
        `dz/dy` / sqrt(`dz/dy`*`dz/dy` + `dz/dx`*`dz/dx`)
      }
    }
  }
}

trait SurfacePointCalculation {
  var lastY = -1
  
  var west = new Array[Int](3)
  var base = new Array[Int](3)
  var east = new Array[Int](3)

  var northRow = 0
  
  def resetCols(row:Int) = {
    northRow = row - 1
    west = new Array[Int](3)
    base = new Array[Int](3)
    east = new Array[Int](3)
  }

  def moveRight(deleteEast:Boolean) = {
    val tmp = west
    west = base
    base = east
    east = tmp

    if(deleteEast) {
      east = new Array[Int](3)
    }
  }

  def add(r:Raster,x:Int,y:Int) {
    if(x == 0) {
      base(y - northRow) = r.get(x,y)
    } else {
      east(y - northRow) = r.get(x,y)
    }
  }

  def calcSurface(s:SurfacePoint,cellWidth:Double,cellHeight:Double):Unit = {
    if(base(1) == NODATA) return 

    s.`dz/dx` = (east(0) + 2*east(1) + east(2) - west(0) - 2*west(1) - west(2)) / (8 * cellWidth)
    s.`dz/dy` = (west(2) + 2*base(2) + east(2) - west(0) - 2*base(0) - east(0)) / (8 * cellHeight)
  }
}
