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

import geotrellis._
import geotrellis.feature.Extent
import geotrellis.source._
import geotrellis.raster._

import org.scalatest._
import geotrellis.testkit._

class FillSpec extends FunSpec 
                  with Matchers 
                  with TestEngine 
                  with TileBuilders {
  describe("Fill"){
    it("Returns a new raster with sinks removed"){
      var ncols = 3
      var nrows = 3
      val re = RasterExtent(Extent(0,0,1,1),1,1,ncols,nrows)
      val m = IntArrayTile(Array[Int](
            1,2,3,
            4,55,6,
            7,8,9),
            ncols,nrows)

      val inRaster = Raster(m, re)
      val o = IntArrayTile(Array[Int](
            1,2,3,
            4,5,6,
            7,8,9),
            ncols,nrows)
      val outRaster = Raster(o, re)
      assertEqual(Fill(inRaster),outRaster )
    } 

    it("Does not remove non-sink even past the threshold"){
      var ncols = 3
      var nrows = 3
      val re = RasterExtent(Extent(0,0,1,1),1,1,ncols,nrows)
      val m = IntArrayTile(Array[Int](
            1,2,100,
            4,55,130,
            80,145,132),
            ncols,nrows)

      val inRaster = Raster(m, re)
      assertEqual(RasterSource(inRaster).fill(FillOptions(50)).get, inRaster)
    } 
  }
}
