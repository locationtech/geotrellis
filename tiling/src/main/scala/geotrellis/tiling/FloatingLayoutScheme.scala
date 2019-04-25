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

package geotrellis.tiling


import geotrellis.raster._
import geotrellis.vector.{Extent, ProjectedExtent}

object FloatingLayoutScheme {
  val DEFAULT_TILE_SIZE = 256

  def apply(): FloatingLayoutScheme =
    apply(DEFAULT_TILE_SIZE)

  def apply(tileSize: Int): FloatingLayoutScheme =
    apply(tileSize, tileSize)

  def apply(tileCols: Int, tileRows: Int): FloatingLayoutScheme =
    new FloatingLayoutScheme(tileCols, tileRows)
}

class FloatingLayoutScheme(val tileCols: Int, val tileRows: Int) extends LayoutScheme {
  def levelFor(extent: Extent, cellSize: CellSize) =
    0 -> LayoutDefinition(GridExtent[Long](extent, cellSize), tileCols, tileRows)

  // TODO: Fix type system so that there is no runtime error
  def zoomOut(level: LayoutLevel) =
    throw new UnsupportedOperationException("zoomOut not supported for FloatingLayoutScheme")

  def zoomIn(level: LayoutLevel) =
    throw new UnsupportedOperationException("zoomIn not supported for FloatingLayoutScheme")
}
