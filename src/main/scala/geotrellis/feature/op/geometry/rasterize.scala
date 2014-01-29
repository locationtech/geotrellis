package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._

import scala.language.higherKinds

/**
 * Invoke a function on each cell under provided features.
 *
 * This function is a closure that returns Unit; all results are a side effect of this function.
 * 
 * @note                    The function f should modify a mutable variable as a side effect.
 *                          While not ideal, this avoids the unavoidable boxing that occurs when a 
 *                          Function3 returns a primitive value.
 * 
 * @param feature  Feature for calculation
 * @param re       RasterExtent to use for iterating through cells
 * @param f        A function that takes (col:Int, row:Int, rasterValue:Int, feature:Feature)
 */
case class ForEachCellByFeature[G[_] <: Geometry[_], D](feature:Op[G[D]], re:Op[RasterExtent])(f: Callback[G,D])
     extends Op2(feature,re)({
  (feature, re) =>
    Result(Rasterizer.foreachCellByFeature(feature,re)(f))
})

case class RasterizeWithValue[D](feature:Op[Geometry[D]], re:Op[RasterExtent], value:Op[Int])
     extends Op3(feature,re,value)({
  (feature, re, value) =>
    Result(Rasterizer.rasterizeWithValue(feature, re, value))
})

case class Rasterize[D](feature:Op[Geometry[D]], re:Op[RasterExtent])(f:Transformer[Geometry,D,Int])
     extends Op2(feature,re)({
  (feature, re) =>
    Result(Rasterizer.rasterize(feature,re)(f))
})
