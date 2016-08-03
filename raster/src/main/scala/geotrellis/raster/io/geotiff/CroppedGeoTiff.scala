package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._

trait CroppedGeoTiff extends WindowedProperties with SegmentWindowProperties

trait WindowedProperties {
  def colMin(implicit gridBounds: GridBounds): Int = gridBounds.colMin
  def rowMin(implicit gridBounds: GridBounds): Int = gridBounds.rowMin
  def colMax(implicit gridBounds: GridBounds): Int = gridBounds.colMax
  def rowMax(implicit gridBounds: GridBounds): Int = gridBounds.rowMax
  def width(implicit gridBounds: GridBounds): Int = gridBounds.width
  def height(implicit gridBounds: GridBounds): Int = gridBounds.height

  def tileWidth(implicit segmentLayout: GeoTiffSegmentLayout): Int =
    segmentLayout.tileLayout.tileCols
}

trait SegmentWindowProperties extends WindowedProperties {

  class SegmentProperties(segment: Int, segmentLayout: GeoTiffSegmentLayout, gridBounds: GridBounds) {
    def colMin: Int = gridBounds.colMin
    def rowMin: Int = gridBounds.rowMin
    def colMax: Int = gridBounds.colMax
    def rowMax: Int = gridBounds.rowMax
    def width: Int = gridBounds.width
    def height: Int = gridBounds.height

    def segmentTransform = segmentLayout.getSegmentTransform(segment)
    def totalCols: Int = segmentLayout.totalCols
    def tileWidth: Int = segmentLayout.tileLayout.tileCols
    def segmentCols: Int = segmentTransform.segmentCols
    def segmentRows: Int = segmentTransform.segmentRows
    def colStart: Int = segmentTransform.indexToCol(0)
    def rowStart: Int = segmentTransform.indexToRow(0)

    def colEnd: Int =
      if (segmentLayout.isStriped)
        totalCols
      else
        (colStart + segmentCols) - 1

    def rowEnd: Int =
      if (segmentLayout.isStriped)
        (segmentRows * segment) + segmentRows
      else
          (rowStart + segmentRows) - 1

    def segmentGridBounds: GridBounds = GridBounds(colStart, rowStart, colEnd, rowEnd)
    def intersection: GridBounds = gridBounds.intersection(segmentGridBounds).get
    def start: Int = {
      if (segmentLayout.isStriped) {
        if (rowStart < rowMin)
          ((rowMin - rowStart) * totalCols) + colMin
        else
          colMin
      } else {
        if (colStart <= colMin && rowStart <= rowMin && rowStart != 0)
          ((rowMin - rowStart) * tileWidth) + colMin
        else if (colStart <= colMin && rowStart > rowMin)
          colMin - colStart
        else if (colStart > colMin && rowStart <= rowMin)
          (rowMin - rowStart) * tileWidth
        else
          0
      }
    }

    def end: Int = {
      if (segmentLayout.isStriped) {
        if (rowEnd > rowMax)
          ((rowMax - rowStart) * totalCols) + colMax + 1
        else
          segmentLayout.getSegmentSize(segment)
      } else {
        start + (tileWidth * (intersection.rowMax - intersection.rowMin)) + intersection.width
      }
    }

    def diff: Int =
      if (segmentLayout.isStriped)
        width
      else
        intersection.width
  }

  def segmentTransform(implicit segment: Int, segmentLayout: GeoTiffSegmentLayout) =
    segmentLayout.getSegmentTransform(segment)
  
  def totalCols(implicit segment: Int, segmentLayout: GeoTiffSegmentLayout) =
    segmentLayout.totalCols
  
  def segmentCols(implicit segment: Int, segmentLayout: GeoTiffSegmentLayout) =
    segmentTransform.segmentCols

  def segmentRows(implicit segment: Int, segmentLayout: GeoTiffSegmentLayout) =
    segmentTransform.segmentRows

  def colStart(implicit segment: Int, segmentLayout: GeoTiffSegmentLayout): Int =
    segmentTransform.indexToCol(0)

  def rowStart(implicit segment: Int, segmentLayout: GeoTiffSegmentLayout): Int =
    segmentTransform.indexToRow(0)

  def colEnd(implicit segment: Int, segmentLayout: GeoTiffSegmentLayout): Int =
    if (segmentLayout.isStriped)
      totalCols
    else
      (colStart + segmentCols) - 1

  def rowEnd(implicit segment: Int, segmentLayout: GeoTiffSegmentLayout): Int =
    if (segmentLayout.isStriped)
      (segmentRows * segment) + segmentRows
    else
      (rowStart + segmentRows) - 1

  def segmentGridBounds(implicit segment: Int, segmentLayout: GeoTiffSegmentLayout): GridBounds =
    GridBounds(colStart, rowStart, colEnd, rowEnd)
    
  def start(implicit segment: Int, gridBounds: GridBounds, segmentLayout: GeoTiffSegmentLayout): Int = {
      if (segmentLayout.isStriped) {
        if (rowStart < rowMin)
          ((rowMin - rowStart) * totalCols) + colMin
        else
          colMin
      } else {
        if (colStart <= colMin && rowStart <= rowMin && rowStart != 0)
          ((rowMin - rowStart) * tileWidth) + colMin
        else if (colStart <= colMin && rowStart > rowMin)
          colMin - colStart
        else if (colStart > colMin && rowStart <= rowMin)
          (rowMin - rowStart) * tileWidth
        else
          0
      }
    }

  /*
  def start(implicit segment: Int, gridBounds: GridBounds, segmentLayout: GeoTiffSegmentLayout): Int = {
    if (segmentLayout.isStriped) {
      if (rowStart < rowMin)
        ((rowMin - rowStart) * totalCols) + colMin
      else
        colMin
    } else {
      if (colStart <= colMin && rowStart <= rowMin && rowStart != 0)
        ((rowMin - rowStart) * tileWidth) + colMin
      else if (colStart <= colMin && rowStart > rowMin)
        colMin - colStart
      else if (colStart > colMin && rowStart <= rowMin)
        (rowMin - rowStart) * tileWidth
      else
        0
    }
  }
  */

  def end(implicit segment: Int, gridBounds: GridBounds, segmentLayout: GeoTiffSegmentLayout): Int = {
    if (segmentLayout.isStriped) {
      if (rowEnd > rowMax)
        ((rowMax - rowStart) * totalCols) + colMax + 1
      else
        segmentLayout.getSegmentSize(segment)
    } else {
      if (colStart <= colMin && colEnd <= colMax && rowEnd < rowMax)
        segmentRows * tileWidth
      else if (colStart <= colMin && colEnd <= colMax && rowEnd >= rowMax)
        (((rowMax - 1) - rowStart) * tileWidth) - (colMax - colEnd)
      else if (colEnd > colMax && rowEnd <= rowMax)
        ((rowEnd - rowStart) * tileWidth) - (colEnd - colMax)
      else
        ((rowMax - rowStart) * tileWidth) + (colEnd - colMax) + 1
    }
  }
  
  def diff(implicit segment: Int, gridBounds: GridBounds, segmentLayout: GeoTiffSegmentLayout): Int = {
    if (colStart <= colMin && colEnd <= colMax)
      colEnd - colMin
    else if (colStart >= colMin && colEnd <= colMax)
      tileWidth
    else if (colStart >= colMin && colEnd >= colMax)
      (colMax - colStart) + 1
    else
      (colMax - colMin) + 1
  }
}
