/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster.TileNeighbors

/** Computes the next step of Conway's Game of Life for a given [[Raster]].
 *
 * @param    r      Raster that represents a state of Conway's Game of Life,
 *                  where NODATA values are dead cells and any other value
 *                  is counted as alive cells.
 * @param    tns    TileNeighbors that describe the neighboring tiles.
 *
 * @see A description of Conway's Game of Life can be found on
 * [[http://en.wikipedia.org/wiki/Conway's_Game_of_Life wikipedia]].
 */
case class Conway(r:Op[Raster],tns:Op[TileNeighbors]) extends FocalOp[Raster](r,Square(1),tns)({
  (r,n) => new CellwiseCalculation[Raster] with ByteRasterDataResult {
    var count = 0

    def add(r:Raster, x:Int, y:Int) = {
      val z = r.get(x,y)
      if (isData(z)) {
        count += 1
      }
    }

    def remove(r:Raster, x:Int, y:Int) = {
      val z = r.get(x,y)
      if (isData(z)) {
        count -= 1
      }
    } 

    def setValue(x:Int,y:Int) = data.set(x,y, if(count == 3 || count == 2) 1 else NODATA)
    def reset() = { count = 0 }
  }
})

object Conway {
  def apply(r:Op[Raster]):Conway = new Conway(r, TileNeighbors.NONE)
}
