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

package geotrellis.raster.op.hydrology

import geotrellis.feature.Extent
import geotrellis.raster._

import org.scalatest._
import geotrellis.testkit._

class AccumulationSpec extends FunSpec 
                          with Matchers 
                          with TestEngine 
                          with TileBuilders {

  describe("Accumulation"){
    it("Calulates the accumulation of water using a flow dirrection raster") {
      var ncols = 6
      var nrows = 6
      val extent = Extent(0,0,1,1)

      val inTile = 
        IntArrayTile(Array[Int](
            2,2,2,4,4,8,
            2,2,2,4,4,8,
            1,1,2,4,8,4,
            128,128,1,2,4,8,
            2,2,1,4,4,4,
            1,1,1,1,4,16),
            ncols,nrows)

      val outTile = 
        IntArrayTile(Array[Int](
            0,0,0,0,0,0,
            0,1,1,2,2,0,
            0,3,7,5,4,0,
            0,0,0,20,0,1,
            0,0,0,1,24,0,
            0,2,4,7,34,1),
            ncols,nrows)

      assertEqual(RasterSource(inTile, extent).accumulation.get, outTile)
    } 

    it("Calulates the accumulation of water using a flow dirrection raster using multiple flow directions") {
      var ncols = 6
      var nrows = 6
      val extent = Extent(0,0,1,1)

      val inTile = 
        IntArrayTile(Array[Int](
            3,3,3,3,3,3,
            3,3,3,3,3,3,
            3,3,3,3,3,3,
            3,3,3,3,3,3,
            3,3,3,3,3,3,
            3,3,3,3,3,3),
            ncols,nrows)

      val outTile = 
        IntArrayTile(Array[Int](
            0,1,2,3,4,5,
            0,2,5,9,14,20,
            0,2,6,13,24,40,
            0,2,6,14,29,55,
            0,2,6,14,30,61,
            0,2,6,14,30,62),
            ncols,nrows)

      assertEqual(RasterSource(inTile, extent).accumulation.get, outTile)
    } 
  }
}
