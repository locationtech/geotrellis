package geotrellis.logic

import geotrellis._
import scala.annotation.tailrec

case class RasterMap(r:Op[Raster])(f:Int => Int) extends Op1(r)({
  r => Result(r.map(f))
})

case class RasterMapIfSet(r:Op[Raster])(f:Int => Int) extends Op1(r)({
  r => Result(r.mapIfSet(f)) 
})

case class RasterDualMapIfSet(r:Op[Raster])(f:Int => Int)(g:Double => Double) extends Op1(r)({
  r => Result(r.dualMapIfSet(f)(g)) 
})

case class RasterDualMap(r:Op[Raster])(f:Int => Int)(g:Double => Double) extends Op1(r)({
  r => Result(r.dualMap(f)(g))
})

case class RasterCombine(r1:Op[Raster], r2:Op[Raster])(f:(Int,Int) => Int) extends Op2(r1, r2)({
  (r1, r2) => Result(r1.combine(r2)(f))
})

case class RasterDualCombine(r1:Op[Raster], r2:Op[Raster])(f:(Int,Int) => Int)(g:(Double,Double) => Double) extends Op2(r1,r2)({
  (r1, r2) => Result(r1.dualCombine(r2)(f)(g))
})

case class RasterDualReduce(rasters:Seq[Op[Raster]])(f:(Int,Int) => Int)(g:(Double,Double) => Double) extends Operation[Raster] {
  def _run(context:Context) = runAsync(rasters.toList)

  val nextSteps:Steps = {
    case rasters:List[_] => handleRasters(rasters.asInstanceOf[List[Raster]])
  }

  @tailrec final def reduce(d:RasterData, rasters:List[Raster]):RasterData = {
    rasters match {
      case Nil => d
      case r :: rs => if (r.isFloat) {
        reduceDouble(d.combineDouble(r.data)(g), rs)
      } else {
        reduce(d.combine(r.data)(f), rs)
      }
    }
  }

  @tailrec final def reduceDouble(d:RasterData, rasters:List[Raster]):RasterData = {
    rasters match {
      case Nil => d
      case r :: rs => reduceDouble(d.combineDouble(r.data)(g), rs)
    }
  }

  def handleRasters(rasters:List[Raster]) = {
    val (r :: rs) = rasters
    if (r.isFloat) {
      Result(Raster(reduceDouble(r.data, rs), r.rasterExtent))
    } else {
      Result(Raster(reduce(r.data, rs), r.rasterExtent))
    }
  }
}
