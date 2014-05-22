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

package geotrellis.raster.op.global

import geotrellis._
import geotrellis.feature._
import geotrellis.raster._
import geotrellis.source._

trait GlobalOpMethods[+Repr <: RasterSource] { self: Repr =>
  def rescale(newMin:Int,newMax:Int) = {
    self.global { r =>
      val (min,max) = r.findMinMax
      r.normalize(min,max,newMin,newMax)
    }
  }

  def toVector() = 
    self.converge.mapOp(ToVector(_))

  def asArray() = 
    self.converge.mapOp(AsArray(_))

  def regionGroup(options:RegionGroupOptions = RegionGroupOptions.default) =
    self.converge.mapOp(RegionGroup(_,options))

  def verticalFlip() =
    self.globalOp(VerticalFlip(_))

  def costDistance(points: Seq[(Int,Int)]) = 
    self.globalOp(CostDistance(_,points))

  def convolve(kernel:Kernel) =
    self.globalOp(Convolve(_,kernel))

  def viewshed(p: Point, exact: Boolean = false) =
    if(exact)
      self.global(Viewshed(_, p))
    else
      self.global(ApproxViewshed(_, p))

  def viewshedOffsets(p: Point, exact: Boolean = false) =
    if(exact)
      self.global(Viewshed.offsets(_, p))
    else
      self.global(ApproxViewshed.offsets(_, p))
}
