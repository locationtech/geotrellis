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

package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import spire.math._

/**
 * This case class is used to determine where an intersection between a
 * segment of a GeoTiffTile and of a GridBounds begins and ends.
 *
 * @param segment: The [[GridBounds]] of the [[GeoTiffSegment]].
 * @param intersection: The [[GridBounds]] that is intersectin with the segment.
 * @param segmentLayout: The [[GeoTiffSegmentLayout]].
  * @return A new instance of Intersection
 */
case class Intersection(segment: GridBounds, window: GridBounds,
  segmentLayout: GeoTiffSegmentLayout) {

  val segmentWidth = segment.width - 1
  val tileWidth = segmentLayout.tileLayout.tileCols

  def cellOffset(col: Int, row: Int): Int =
    row * segment.width + col

  def start = startOffset
  val startOffset =
    if (segmentLayout.isStriped)
      cellOffset(window.colMin, window.rowMin)
    else
      if (segment.colMin < window.colMin && segment.rowMin < window.rowMin)
        ((window.rowMin - segment.rowMin) * tileWidth) + window.colMin
      else if (segment.colMin < window.colMin && segment.rowMin == window.rowMin)
        window.colMin
      else if (segment.colMin == window.colMin && segment.rowMin < window.rowMin)
        (window.rowMin - segment.rowMin) * tileWidth
      else
        0

  def end = endOffset
  val endOffset =
    if (segmentLayout.isStriped) {
      cellOffset(window.colMax, window.rowMax)
    } else
      startOffset + (tileWidth * (window.rowMax - window.rowMin)) + window.width - 1
}
