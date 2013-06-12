package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.process._
import scala.math.{max,min}

/**
  * Returns a tuple with the minimum and maximum value of a raster.
  *
  * @note    Currently does not support finding double Min and Max values.
  *          If used with a raster with a double data type (TypeFloat,TypeDouble),
  *          will find the integer min and max of the rounded cell values.
  */
case class GetMinMax(r:Op[Raster]) extends logic.TileReducer1[(Int,Int)] {
  type B = (Int,Int)

  val maxOp = op {
    (r:Raster) => Result(r.findMinMax) 
  }

  def mapper(r:Op[Raster]) = logic.AsList(maxOp(r))
  def reducer(results:List[(Int,Int)]) = results.reduceLeft( (a,b) => (min(a._1, b._1), max(a._2, b._2) )) 
}
