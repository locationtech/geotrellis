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
import geotrellis.vector._

/** A LayoutScheme is something that provides LayoutLevels based on an integer id or
  * an extent and cellsize. The resolution of the tiles for the LayoutLevel returned
  * will not necessarily match the CellSize provided, but an appropriately close
  * selection will be made.
  *
  * It also provides methods for next zoomed out tile layout level.
  */
trait LayoutScheme extends Serializable {
  def levelFor(extent: Extent, cellSize: CellSize): LayoutLevel
  def zoomOut(level: LayoutLevel): LayoutLevel
  def zoomIn(level: LayoutLevel): LayoutLevel
}

case class LayoutLevel(zoom: Int, layout: LayoutDefinition)

object LayoutLevel {
  implicit def fromTuple(tup: (Int, LayoutDefinition)): LayoutLevel = LayoutLevel(tup._1, tup._2)
}
