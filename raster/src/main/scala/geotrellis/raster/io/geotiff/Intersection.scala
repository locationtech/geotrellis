package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._

trait Intersection {
  case class Intersection(segmentGridBounds: GridBounds, intersection: GridBounds, segmentLayout: GeoTiffSegmentLayout) {
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
}
