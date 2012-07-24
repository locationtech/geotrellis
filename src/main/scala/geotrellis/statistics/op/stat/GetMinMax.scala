package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.process._
import geotrellis._
import geotrellis._


/**
  * Find the minimum and maximum value of a raster. 
  */
case class GetMinMax(r:Op[Raster]) extends Op1(r) ({
  (r) => Result(r.findMinMax) 
})
