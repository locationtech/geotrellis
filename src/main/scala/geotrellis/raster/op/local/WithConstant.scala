package geotrellis.raster.op.local

import geotrellis._
import geotrellis._
import geotrellis.process._

import scala.math.{max, min, pow}

///**
// * Raise each cell to the cth power.
// */
//case class PowConstant(r:Op[Raster], c:Op[Int]) extends Op2(r,c) ({
//  (r,c) => Result(r.dualMapIfSet(pow(_, c).toInt)(pow(_, c)))
//})
//
//case class PowDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r,c) ({
//  (r,c) => Result(r.dualMapIfSet(pow(_, c).toInt)(pow(_, c)))
//})
