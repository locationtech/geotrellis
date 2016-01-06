package geotrellis.raster

import geotrellis.vector.Extent
import scala.math.{min, max, ceil}

trait GridDefinition {
  def extent: Extent
  def cellwidth: Double
  def cellheight: Double

  def cellSize = CellSize(cellwidth, cellheight)

  def toGridExtent: GridExtent = GridExtent(extent, cellwidth, cellheight)

  /** Creates a RasterExtent out of this GridExtent.
    *
    * @note                      Use with caution: if the number of columns or rows are larger than Int.MaxValue,
    *                            this will throw an exception. Also, if columns * rows > Int.MaxValue, this will
    *                            create a RasterExtent for a raster that could not be backed by any of the Array-backed 
    *                            tile types.
    */
  def toRasterExtent(): RasterExtent = {
    val targetCols = math.round(extent.width / cellwidth).toLong
    val targetRows = math.round(extent.height / cellheight).toLong
    if(targetCols > Int.MaxValue) {
      throw new GeoAttrsError(s"Cannot convert GridExtent into a RasterExtent: number of columns exceeds maximum integer value ($targetCols > ${Int.MaxValue})")
    }
    if(targetRows > Int.MaxValue) {
      throw new GeoAttrsError(s"Cannot convert GridExtent into a RasterExtent: number of rows exceeds maximum integer value ($targetRows > ${Int.MaxValue})")
    }

    RasterExtent(extent, cellwidth, cellheight, targetCols.toInt, targetRows.toInt)
  }

  /**
   * Returns a GridExtent that lines up with this grid' resolution
   * and grid layout.
   * i.e., the resulting GridExtent will not have the given extent,
   * but will have the smallest extent such that the whole of
   * the given extent is covered, that lines up with the grid.
   */
  def createAlignedGridExtent(targetExtent: Extent): GridExtent = {
    val xmin = extent.xmin + (math.floor((targetExtent.xmin - extent.xmin) / cellwidth) * cellwidth)
    val xmax = extent.xmax - (math.floor((extent.xmax - targetExtent.xmax) / cellwidth) * cellwidth)
    val ymin = extent.ymin + (math.floor((targetExtent.ymin - extent.ymin) / cellheight) * cellheight)
    val ymax = extent.ymax - (math.floor((extent.ymax - targetExtent.ymax) / cellheight) * cellheight)

    GridExtent(Extent(xmin, ymin, xmax, ymax), cellwidth, cellheight)
  }

  /**
   * Returns a RasterExtent that lines up with this RasterExtent's resolution,
   * and grid layout.
   * i.e., the resulting RasterExtent will not have the given extent,
   * but will have the smallest extent such that the whole of
   * the given extent is covered, that lines up with the grid.
   */
  def createAlignedRasterExtent(targetExtent: Extent): RasterExtent =
    createAlignedGridExtent(targetExtent).toRasterExtent

  /**
    * Gets the Extent that matches the grid bounds passed in, aligned with this RasterExtent.
    * 
    * @param     gridBounds      The extent to get the grid bounds for
    * @param     clamp          Determines whether or not to clamp the Extent to the
    *                           extent of this RasterExtent; defaults to true. If true, the
    *                           returned extent will be contained by this RasterExtent's extent,
    *                           if false, the Extent returned can be outside of this RasterExtent's extent.
    */
  def extentFor(gridBounds: GridBounds, clamp: Boolean = true): Extent = {
    val xmin = gridBounds.colMin * cellwidth + extent.xmin
    val ymax = extent.ymax - (gridBounds.rowMin * cellheight)
    val xmax = xmin + (gridBounds.width * cellwidth)
    val ymin = ymax - (gridBounds.height * cellheight)

    if(clamp) {
      Extent(
        max(min(xmin, extent.xmax), extent.xmin),
        max(min(ymin, extent.ymax), extent.ymin),
        max(min(xmax, extent.xmax), extent.xmin),
        max(min(ymax, extent.ymax), extent.ymin)
      )
    } else {
      Extent(xmin, ymin, xmax, ymax)
    }
  }
}
