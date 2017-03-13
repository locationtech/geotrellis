/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._

object Conway {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    new CellwiseCalculation[Tile](tile, n, bounds, TargetCell.All)
      with ByteArrayTileResult
    {
      var count = 0

      def add(r: Tile, x: Int, y: Int) = {
        val z = r.get(x, y)
        if (isData(z)) {
          count += 1
        }
      }

      def remove(r: Tile, x: Int, y: Int) = {
        val z = r.get(x, y)
        if (isData(z)) {
          count -= 1
        }
      }

      def setValue(x: Int, y: Int) = resultTile.set(x, y, if(count == 3 || count == 2) 1 else NODATA)
      def reset() = { count = 0 }
    }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, bounds).execute()
}
