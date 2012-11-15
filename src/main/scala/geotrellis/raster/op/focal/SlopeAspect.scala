package geotrellis.raster.op.focal

import geotrellis._
import geotrellis._
import geotrellis.process._
import geotrellis.Raster

import scala.math._

import Angles._

protected[focal] abstract class SlopeAspectCalculator[@specialized(Int,Double) D](zFactor:Double,
										  cellWidth:Double,
										  cellHeight:Double) extends FocalCalculation[D] {
  var west = new Array[Int](3)
  var base = new Array[Int](3)
  var east = new Array[Int](3)

  var northRow = 0

  override def center(col:Int, row:Int, r:Raster) {
    northRow = row - 1

    val tmp = west
    west = base
    base = east
    east = tmp
  }

  def clear() {
    west = new Array[Int](3)
    base = new Array[Int](3)
    east = new Array[Int](3)
  }

  def remove(col:Int, row:Int, r:Raster) {}
  def add(col:Int, row:Int, r:Raster) {
    east(row - northRow) = r.get(col, row)
  }

  def getSlopeAndAspect: (Double, Double) = {
    if (base(1) == NODATA) return (Double.NaN, Double.NaN)

    // east - west
    val `dz/dx` = (east(0) + 2*east(1) + east(2) - west(0) - 2*west(1) - west(2)) / (8 * cellWidth)

    // south - north
    val `dz/dy` = (west(2) + 2*base(2) + east(2) - west(0) - 2*base(0) - east(0)) / (8 * cellHeight)

    val slope = atan(zFactor * sqrt(`dz/dx` * `dz/dx` + `dz/dy` * `dz/dy`))
    
    // Determine aspect based off of dz/dx and dz/dy
    var aspect = atan2(`dz/dy`, -`dz/dx`)
    if(`dz/dx` != 0) {
      if(aspect < 0) {
	aspect = (2*Pi) + aspect
      }
    } else {
      if(`dz/dy` > 0) {
	aspect = (Pi / 2)
      } else if(`dz/dy` < 0) {
	aspect = (2*Pi) - (Pi / 2)
      }
    }

    (slope,aspect)
  }
}

case class Aspect(r:Op[Raster], zFactor:Op[Double]) extends Op2(r, zFactor) ({
  (r, zf) =>
    val newCalc = () => new AspectCalc(zf, r.rasterExtent.cellwidth, r.rasterExtent.cellheight)
    FocalOp.getResultDouble(r, Sliding, Square(1), newCalc)
})

protected[focal] case class AspectCalc(zFactor:Double, 
				       cellWidth:Double, 
				       cellHeight:Double) extends SlopeAspectCalculator[Double](zFactor,cellWidth,cellHeight) {
  def getResult:Double = {
    if (base(1) == NODATA) return NODATA

    val (_,aspect) = getSlopeAndAspect
    degrees(aspect)
  }
}

case class Slope(r:Op[Raster], zFactor:Op[Double]) extends Op2(r, zFactor) ({
  (r, zf) =>
    val newCalc = () => new SlopeCalc(zf, r.rasterExtent.cellwidth, r.rasterExtent.cellheight)
    FocalOp.getResultDouble(r, Sliding, Square(1), newCalc)
})

protected[focal] case class SlopeCalc(zFactor:Double,
				      cellWidth:Double,
				      cellHeight:Double) extends SlopeAspectCalculator[Double](zFactor,cellWidth,cellHeight) {
  def getResult:Double = {
    if(base(1) == NODATA) return NODATA
    val (slope,_) = getSlopeAndAspect
    degrees(slope)

  }
}

