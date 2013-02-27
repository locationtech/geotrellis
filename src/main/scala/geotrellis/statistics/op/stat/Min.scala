package geotrellis.statistics.op.stat

import scala.math.{min, max}

import geotrellis._

case class Min(r:Op[Raster]) extends logic.TileReducer1[Int] {
  type B = Int

  def mapper(r: Op[Raster]) = {
    println("in mapper")
    logic.AsList(Min(r))
  }
  def reducer(mapResults:List[Int]) = {
    println("in reducer")
    mapResults.reduceLeft(min)
  }
}
