package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.process._
import geotrellis._
import geotrellis._


/**
  * Find the minimum and maximum value of a raster.
  *
  * @note    Currently does not support finding double Min and Max values.
  *          If used with a raster with a double data type (TypeFloat,TypeDouble),
  *          will find the integer min and max of the rounded cell values.
  */
case class GetMinMax(r:Op[Raster]) extends Op1(r) ({
  (r) => Result(r.findMinMax) 
})
