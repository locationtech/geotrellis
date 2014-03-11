/***
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
 ***/

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

import Angles._

/** Calculates the aspect of each cell in a raster.
  *
  * Aspect is the direction component of a gradient vector. It is the
  * direction in degrees of which direction the maximum change in direction is pointing.
  * It is defined as the directional component of the gradient vector and is the
  * direction of maximum gradient of the surface at a given point. It uses Horn's method
  * for computing aspect.
  *
  * As with slope, aspect is calculated from estimates of the partial derivatives dz/dx and dz/dy.
  *
  * Aspect is computed in degrees from due north, i.e. as an azimuth in degrees not radians.
  * The expression for aspect is:
  * {{{
  * val aspect = 270 - 360/(2*Pi) * atan2(`dz/dy`, - `dz/dx`)
  * }}}
  * @param   raster     Raster for which to compute the aspect.
  *
  * @see [[SurfacePoint]] for aspect calculation logic.
  * @note Paraphrased from
  * [[http://goo.gl/JCnNP Geospatial Analysis - A comprehensive guide]]
  * (Smit, Longley, and Goodchild)
  */
case class Aspect(r:Op[Raster],neighbors:Op[TileNeighbors]) 
    extends FocalOp[Raster](r,Square(1),neighbors)({
  (r,n) => new SurfacePointCalculation[Raster] with DoubleRasterDataResult {
    def setValue(x:Int,y:Int,s:SurfacePoint) {
      data.setDouble(x,y,degrees(s.aspect))
    }
  }
}) with FocalOperation[Raster]

object Aspect {
  def apply(r:Op[Raster]):Aspect = Aspect(r,Literal(TileNeighbors.NONE))
}
