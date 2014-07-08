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

import geotrellis.raster._
import geotrellis.feature._
import geotrellis.engine._
import geotrellis.testkit._

import org.scalatest._
class ZonalSummaryOpMethodsSpec extends FunSpec
                                   with Matchers
                                   with TestEngine
                                   with TileBuilders {  
  describe("ZonalSummaryOpMethods") {
    it("computes mean on double raster source with cached results") {
      val rs =
        createRasterSource(
          Array(  0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,

            0.11,0.12,0.13,   0.14,NaN,0.15,  NaN, 0.16,0.17,
            0.11,0.12,0.13,   NaN,0.15,NaN,   0.17, 0.18,0.19,

            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,

            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9
          ),
          3,4,3,2)

      val tr = rs.get
      val re = rs.rasterExtent.get

      val p = {
        val polyPoints = Seq(
          Point(re.gridToMap(1,1)), Point(re.gridToMap(2,0)), Point(re.gridToMap(4,0)), Point(re.gridToMap(7,2)),
          Point(re.gridToMap(6,6)), Point(re.gridToMap(1,6)), Point(re.gridToMap(0,3)), Point(re.gridToMap(1,1))
        )
        Polygon(Line(polyPoints))
      }

      val cached =
        rs.map(tile => { println("using cached result") ; MeanResult.fromFullTileDouble(tile)})
          .cached

      val withCached = rs.zonalMeanDouble(p,cached)
      val withoutCached = rs.zonalMeanDouble(p)

      withCached.get should be (withoutCached.get)
    }
  }
}
