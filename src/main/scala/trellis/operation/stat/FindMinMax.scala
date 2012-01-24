package trellis.operation

import trellis._
import trellis.process._

/**
  * Find the minimum and maximum value of a raster. 
  */
case class FindMinMax(r:Op[IntRaster]) extends Op1(r) ({
  (r) => Result(r.findMinMax) 
})
