/*
 * Copyright 2018 Azavea
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

package geotrellis.tiling

import geotrellis.raster.CellSize
import geotrellis.vector.Extent

/** Layout scheme for building a local power of 2 pyramid.
  * Zooming out will reduce raster pixel resolution by 2, while using minimum number of tiles.
  * Layouts produced by this scheme will not be power of 2 however.
  * Uneven layouts will pyramid up until they are reduced to a single tile.
  */
class LocalLayoutScheme extends LayoutScheme {
  import LocalLayoutScheme._
  import math._

  def zoomOut(level: LayoutLevel): LayoutLevel = {
    val LayoutLevel(zoom, LayoutDefinition(extent, tileLayout)) = level
    require(zoom > 0)
    // layouts may be uneven, don't let the short dimension go to 0
    val outCols = max(1, pyramid(tileLayout.layoutCols))
    val outRows = max(1, pyramid(tileLayout.layoutRows))
    val outLayout = LayoutDefinition(extent, tileLayout.copy(layoutCols = outCols, layoutRows = outRows))
    LayoutLevel(zoom - 1, outLayout)
  }

  // not used in Pyramiding
  def zoomIn(level: LayoutLevel): LayoutLevel = ???
  def levelFor(extent: Extent, cellSize: CellSize): LayoutLevel = ???
}

object LocalLayoutScheme {
  import math._
  private def pow2(x: Int) = ceil(log(x)/log(2)).toInt
  private def pad2(x: Int) = pow(2, ceil(log(x)/log(2))).toInt

  /** Tiles needed if we reduced resolution by power of 2 */
  def pyramid(tiles: Int): Int = {
    val pad = pad2(tiles)
    val reducedPadded = pad / 2
    ceil(reducedPadded * (tiles.toDouble/pad.toDouble)).toInt
  }

  /** Infer zoom level by how many times this layout can be reduced before becoming a single tile */
  def inferLayoutLevel(ld: LayoutDefinition): Int =
    max(pow2(ld.tileLayout.layoutCols), pow2(ld.tileLayout.layoutRows))
}
