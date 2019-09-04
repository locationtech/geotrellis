/*
 * Copyright 2019 Azavea
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

package geotrellis.layer

import geotrellis.proj4.CRS
import geotrellis.raster.CellSize
import geotrellis.vector.Extent

/** Strategy for selecting LayoutScheme before metadata is collected */
sealed trait LayoutType {
  /** Produce the [[LayoutDefinition]] and zoom level, if applicable, for given raster */
  def layoutDefinitionWithZoom(crs: CRS, extent: Extent, cellSize: CellSize): (LayoutDefinition, Option[Int])

  /** Produce the [[LayoutDefinition]] for given raster */
  def layoutDefinition(crs: CRS, extent: Extent, cellSize: CellSize): LayoutDefinition =
    layoutDefinitionWithZoom(crs, extent, cellSize)._1
}

/** @see [[geotrellis.layer.ZoomedLayoutScheme]] */
case class GlobalLayout(tileSize: Int, zoom: Int, threshold: Double)  extends LayoutType {
  def layoutDefinitionWithZoom(crs: CRS, extent: Extent, cellSize: CellSize) = {
    val scheme = new ZoomedLayoutScheme(crs, tileSize, threshold)
    Option(zoom) match {
      case Some(zoom) =>
        scheme.levelForZoom(zoom).layout -> Some(zoom)
      case None =>
        val LayoutLevel(zoom, ld) = scheme.levelFor(extent, cellSize)
        ld -> Some(zoom)
    }
  }
}

/** @see [[geotrellis.layer.FloatingLayoutScheme]] */
case class LocalLayout(tileCols: Int, tileRows: Int) extends LayoutType {
  def layoutDefinitionWithZoom(crs: CRS, extent: Extent, cellSize: CellSize) = {
    val scheme = new FloatingLayoutScheme(tileCols, tileRows)
    scheme.levelFor(extent, cellSize).layout -> None
  }
}


object LocalLayout {
  def apply(tileSize: Int): LocalLayout =
    LocalLayout(tileSize, tileSize)
}
