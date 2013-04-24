package geotrellis.logic

import geotrellis._

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
