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

/**
 * This case class is used to determine where an intersection between a
 * segment of a GeoTiffTile and of a GridBounds begins and ends.
 *
 * @param segmentGridBounds: The [[GridBounds]] of the [[GeoTiffSegment]].
 * @param intersection: The [[GridBounds]] that is intersectin with the segment.
 * @param segmentLayout: The [[GeoTiffSegmentLayout]].
 *
 * @return A new instance of Intersection
 */
case class Intersection(segmentGridBounds: GridBounds, intersection: GridBounds,
  segmentLayout: GeoTiffSegmentLayout) {

  val sgColMin = segmentGridBounds.colMin
  val sgRowMin = segmentGridBounds.rowMin
  val sgColMax = segmentGridBounds.colMax
  val sgRowMax = segmentGridBounds.rowMax
  val cols = segmentGridBounds.width - 1
  val tileWidth = segmentLayout.tileLayout.tileCols

  val iColMin = intersection.colMin
  val iRowMin = intersection.rowMin
  val iColMax = intersection.colMax
  val iRowMax = intersection.rowMax

  val start =
    if (segmentLayout.isStriped)
      if (sgRowMin < iRowMin)
        ((intersection.rowMin - sgRowMin) * cols) + iColMin
      else
        iColMin
    else
      if (sgColMin < iColMin && sgRowMin < iRowMin)
        ((iRowMin - sgRowMin) * tileWidth) + iColMin
      else if (sgColMin < iColMin && sgRowMin == iRowMin)
        iColMin
      else if (sgColMin == iColMin && sgRowMin < iRowMin)
        (iRowMin - sgRowMin) * tileWidth
      else
        0

  val end =
    if (segmentLayout.isStriped)
      if (iColMax != sgColMax && iRowMax != sgRowMax)
        start + (cols * (intersection.rowMax - intersection.rowMin)) + intersection.width
      else
        start + (cols * (intersection.rowMax - intersection.rowMin))
    else
        start + (tileWidth * (intersection.rowMax - intersection.rowMin)) + intersection.width
}
