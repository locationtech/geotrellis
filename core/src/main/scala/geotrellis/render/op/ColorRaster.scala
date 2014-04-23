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

package geotrellis.render.op

import geotrellis._
import geotrellis.render._

object ColorRaster {
  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Int,Int]]):Op[Raster] = 
    (r,breaksToColors).map { (r,breaksToColors) => 
      IntColorMap(breaksToColors).render(r)
    }.withName("IntColorMap")

  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Int,Int]],
            options:ColorMapOptions):Op[Raster] = 
    (r,breaksToColors).map { (r,breaksToColors) => 
      IntColorMap(breaksToColors,options).render(r)
    }.withName("IntColorMap")


  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Double,Int]])(implicit d:DI):Op[Raster] =
    (r,breaksToColors).map { (r,breaksToColors) => 
      DoubleColorMap(breaksToColors).render(r)
    }.withName("IntColorMap")

  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Double,Int]],
            options:ColorMapOptions)(implicit d:DI):Op[Raster] = 
    (r,breaksToColors).map { (r,breaksToColors) => 
      DoubleColorMap(breaksToColors,options).render(r)
    }.withName("IntColorMap")
}
