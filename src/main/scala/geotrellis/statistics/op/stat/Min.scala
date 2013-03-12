package geotrellis.statistics.op.stat

import scala.math.{min, max}

import geotrellis._

case class Min(r:Op[Raster]) extends logic.TileReducer1[Int] {
  type B = Int

  case class UntiledMin(r:Op[Raster]) extends Op1(r) ({
    (r) => {
      var zmin = Int.MaxValue
      r.foreach(z => if (z != NODATA) zmin = min(z, zmin))
      Result(zmin)
    } 
  })

  def mapper(r: Op[Raster]) = logic.AsList(UntiledMin(r))
  def reducer(mapResults:List[Int]) = mapResults.reduceLeft(min)

}
