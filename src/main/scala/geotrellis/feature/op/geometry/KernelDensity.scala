/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.source._
import geotrellis.feature.Point
import geotrellis.raster.Kernel
import geotrellis.raster.Convolver

/**
 * Computes a Density raster based on the Kernel and set of points provided.
 *
 * @param      points           Sequence of point features who's values will be used to
 *                              compute the density.
 * @param      transform        Function that transforms the point feature's data into
 *                              an Int value.
 * @param      kernel           [[Kernel]] to be used in the computation.
 * @param      rasterExtent     Raster extent of the resulting raster.
 *
 * @note                        KernelDensity does not currently support Double raster data.
 *                              If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                              the data values will be rounded to integers.
 */
case class KernelDensity[D](points:Op[Seq[Point[D]]], 
                            transform:Op[D=>Int],
                            kernel:Op[Kernel],
                            rasterExtent:Op[RasterExtent])
     extends Op4(points,transform,kernel,rasterExtent)({ 
       (points,transform,kernel,rasterExtent) =>
         val convolver = new Convolver(rasterExtent,kernel)
       
         for(point <- points) {
           val col = convolver.rasterExtent.mapXToGrid(point.geom.getX())
           val row = convolver.rasterExtent.mapYToGrid(point.geom.getY())
           convolver.stampKernel(col,row,transform(point.data))
         }                                                       
         Result(convolver.result)
})
