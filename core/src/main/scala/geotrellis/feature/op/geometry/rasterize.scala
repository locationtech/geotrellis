/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * @param geom  Feature for calculation
 * @param re       RasterExtent to use for iterating through cells
 * @param f        A function that takes (col:Int, row:Int, rasterValue:Int, feature:Feature)
 */
case class ForEachCellByFeature[G <: Geometry](geom: Op[G], re: Op[RasterExtent])(f: Callback)
     extends Op2(geom,re)({
  (geom, re) =>
    Result(Rasterizer.foreachCellByFeature(geom, re)(f))
})

case class RasterizeWithValue[G <: Geometry](geom: Op[G], re:Op[RasterExtent], value: Op[Int])
     extends Op3(geom,re,value)({
  (geom, re, value) =>
    Result(Rasterizer.rasterizeWithValue(geom, re, value))
})

case class Rasterize[G <: Geometry](geom: Op[G], re: Op[RasterExtent])(f: Transformer[Int])
     extends Op2(geom,re)({
  (geom, re) =>
    Result(Rasterizer.rasterize(geom,re)(f))
})
