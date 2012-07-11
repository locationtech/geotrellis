package geotrellis.op.stat

import geotrellis._
import geotrellis.process._
import geotrellis._
import geotrellis.op._


/**
  * Find the minimum and maximum value of a raster. 
  */
case class FindMinMax(r:Op[Raster]) extends Op1(r) ({
  (r) => Result(r.findMinMax) 
})
