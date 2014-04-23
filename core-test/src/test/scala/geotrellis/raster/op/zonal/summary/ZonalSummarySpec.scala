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

package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.feature._
import geotrellis.process._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers._

trait ZonalSummarySpec extends FunSpec
                          with ShouldMatchers
                          with TestServer
                          with RasterBuilders {
  val tiledRS = 
    createRasterSource(
      Array(  1, 2, 3,   4, 5, 6,   7, 8, 9,
              1, 2, 3,   4, 5, 6,   7, 8, 9,

             10,11,12,  13,nd,14,  nd,15,16,
             11,12,13,  nd,15,nd,  17,18,19
      ),
      3,2,3,2)

  val tiledRSDouble = 
    createRasterSource(
      Array(  0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
              0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,

             0.11,0.12,0.13,   0.14,NaN,0.15,   NaN, 0.16,0.17,
             0.11,0.12,0.13,    NaN,0.15,NaN,  0.17, 0.18,0.19
      ),
      3,2,3,2)

  val tiledR = get(tiledRS)
  val tiledRDouble = get(tiledRSDouble)

  val poly = {
    val re = tiledR.rasterExtent
    val polyPoints = Seq(
      re.gridToMap(2,1), re.gridToMap(4,0),re.gridToMap(7,2),
      re.gridToMap(5,3), re.gridToMap(2,2),re.gridToMap(2,1)
    )
    Polygon(polyPoints, 0)
  }

//      Polygon vertices are 0's (not contianed cells)
//      X's are contained cells
// 
//       *  *  *    *  *  *    *  *  *
//       *  *  0    X  X  X    *  *  *
// 
//       *  *  0    X  X  X    X  0  *
//       *  *  *    *  *  *    *  *  *  
// 

  val containedCells = Seq(
    (3,1),(4,1),(5,1),
    (3,2),(4,2),(5,2),(6,2)
  )
}
