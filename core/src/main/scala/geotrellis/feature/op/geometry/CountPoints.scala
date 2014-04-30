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
import com.vividsolutions.jts.geom

/**
 * Gives a raster that represents the number of occuring points per cell.
 * 
 *  @param points               Sequence of points to be counted.
 *  @param rasterExtent         RasterExtent of the resulting raster.
 * 
 */
case class CountPoints(points:Op[Seq[Point]], rasterExtent:Op[RasterExtent]) extends Op2(points,rasterExtent) ({
  (points,re) =>
    val array = Array.ofDim[Int](re.cols * re.rows).fill(0)
    for(point <- points) {
      val x = point.x
      val y = point.y
      if(re.extent.containsPoint(x,y)) {
        val index = re.mapXToGrid(x)*re.cols + re.mapYToGrid(y)
        array(index) = array(index) + 1
      }
    }
    Result(Raster(array,re))
})
