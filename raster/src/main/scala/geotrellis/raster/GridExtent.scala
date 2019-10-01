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

package geotrellis.raster

import geotrellis.proj4.{CRS, Transform}
import geotrellis.raster.reproject.Reproject.Options
import geotrellis.raster.reproject.ReprojectRasterExtent
import geotrellis.vector._

import scala.math.{ceil, max, min}
import spire.math.Integral
import spire.implicits._

/**
  * Represents an abstract grid over geographic extent.
  * Critically while the number of cell rows and columns is implied by the constructor arguments,
  * they are intentionally not expressed to avoid Int overflow for large grids.
  */
class GridExtent[@specialized(Int, Long) N: Integral](
  val extent: Extent,
  val cellwidth: Double,
  val cellheight: Double,
  val cols: N,
  val rows: N
) extends Grid[N] with Serializable {
  import GridExtent._

  if (cols <= 0) throw GeoAttrsError(s"invalid cols: $cols")
  if (rows <= 0) throw GeoAttrsError(s"invalid rows: $rows")

  require(
    cols == Integral[N].fromDouble(math.round(extent.width / cellwidth)) &&
    rows == Integral[N].fromDouble(math.round(extent.height / cellheight)),
    s"$extent at $cellSize does not match $dimensions"
  )

  def this(extent: Extent, cols: N, rows: N) =
    this(extent, (extent.width / cols.toDouble), (extent.height / rows.toDouble), cols, rows)

  def this(extent: Extent, cellSize: CellSize) =
    this(extent, cellSize.width, cellSize.height,
      cols = Integral[N].fromDouble(math.round(extent.width / cellSize.width)),
      rows = Integral[N].fromDouble(math.round(extent.height / cellSize.height)))

  def cellSize = CellSize(cellwidth, cellheight)

  /**
  * Combine two different [[RasterExtent]]s (which must have the
  * same cellsizes).  The result is a new extent at the same
  * resolution.
  */
  def combine (that: GridExtent[N]): GridExtent[N] = {
    if (cellwidth != that.cellwidth)
      throw GeoAttrsError(s"illegal cellwidths: $cellwidth and ${that.cellwidth}")
    if (cellheight != that.cellheight)
      throw GeoAttrsError(s"illegal cellheights: $cellheight and ${that.cellheight}")

    val newExtent = extent.combine(that.extent)
    val newRows = ceil(newExtent.height / cellheight)
    val newCols = ceil(newExtent.width / cellwidth)

    new GridExtent[N](newExtent, cellwidth, cellheight,
      cols = Integral[N].fromDouble(newCols),
      rows = Integral[N].fromDouble(newRows))
  }

  /** Convert map coordinate x to grid coordinate column. */
  final def mapXToGridDouble(x: Double): Double = (x - extent.xmin) / cellwidth

  /** Convert map coordinate y to grid coordinate row. */
  final def mapYToGridDouble(y: Double): Double = (extent.ymax - y ) / cellheight

  /** Convert map coordinate x to grid coordinate column. */
  final def mapXToGrid(x: Double): N = Integral[N].fromDouble(floorWithTolerance(mapXToGridDouble(x)))

  /** Convert map coordinate y to grid coordinate row. */
  final def mapYToGrid(y: Double): N = Integral[N].fromDouble(floorWithTolerance(mapYToGridDouble(y)))

  /** Convert map coordinates (x, y) to grid coordinates (col, row). */
  final def mapToGrid(x: Double, y: Double): (N, N) = {
    val col = floorWithTolerance((x - extent.xmin) / cellwidth).toInt
    val row = floorWithTolerance((extent.ymax - y) / cellheight).toInt
    (col, row)
  }

  /** Convert map coordinate tuple (x, y) to grid coordinates (col, row). */
  final def mapToGrid(mapCoord: (Double, Double)): (N, N) =
    mapToGrid(x = mapCoord._1, mapCoord._2)

  /** Convert a point to grid coordinates (col, row). */
  final def mapToGrid(p: Point): (N, N) =
    mapToGrid(p.x, p.y)

  /** The map coordinate of a grid cell is the center point. */
  final def gridToMap(col: N, row: N): (Double, Double) = {
    val x = col.toDouble * cellwidth + extent.xmin + (cellwidth / 2)
    val y = extent.ymax - (row.toDouble * cellheight) - (cellheight / 2)
    (x, y)
  }

  /** For a give column, find the corresponding x-coordinate in the grid of the present [[GridExtent]]. */
  final def gridColToMap(col: N): Double = {
    col.toDouble * cellwidth + extent.xmin + (cellwidth / 2)
  }

  /** For a give row, find the corresponding y-coordinate in the grid of the present [[GridExtent]]. */
  final def gridRowToMap(row: N): Double = {
    extent.ymax - (row.toDouble * cellheight) - (cellheight / 2)
  }

  /**
  * Returns a [[RasterExtent]] with the same extent, but a modified
  * number of columns and rows based on the given cell height and
  * width.
  */
  def withResolution(targetCellWidth: Double, targetCellHeight: Double): GridExtent[N] = {
    val newCols = math.ceil((extent.xmax - extent.xmin) / targetCellWidth)
    val newRows = math.ceil((extent.ymax - extent.ymin) / targetCellHeight)
    new GridExtent(extent, targetCellWidth, targetCellHeight,
      cols = Integral[N].fromDouble(newCols),
      rows = Integral[N].fromDouble(newRows))
  }

  /**
    * Returns a [[GridExtent]] with the same extent, but a modified
    * number of columns and rows based on the given cell height and
    * width.
    */
  def withResolution(cellSize: CellSize): GridExtent[N] =
    withResolution(cellSize.width, cellSize.height)

  /**
   * Returns a [[GridExtent]] with the same extent and the given
   * number of columns and rows.
   */
  def withDimensions(targetCols: N, targetRows: N): GridExtent[N] =
    new GridExtent(extent, targetCols, targetRows)

  /**
    * Gets the GridBounds aligned with this RasterExtent that is the
    * smallest subgrid containing all points within the extent. The
    * extent is considered inclusive on it's north and west borders,
    * exclusive on it's east and south borders.  See [[RasterExtent]]
    * for a discussion of grid and extent boundary concepts.
    *
    * The 'clamp' flag determines whether or not to clamp the
    * GridBounds to the RasterExtent; defaults to true. If false,
    * GridBounds can contain negative values, or values outside of
    * this RasterExtent's boundaries.
    *
    * @param     subExtent      The extent to get the grid bounds for
    * @param     clamp          A boolean
    */
  def gridBoundsFor(subExtent: Extent, clamp: Boolean = true): GridBounds[N] = {
    // West and North boundaries are a simple mapToGrid call.
    val colMin: N = mapXToGrid(subExtent.xmin)
    val rowMin: N = mapYToGrid(subExtent.ymax)

    // If South East corner is on grid border lines, we want to still only include
    // what is to the West and\or North of the point. However if the border point
    // is not directly on a grid division, include the whole row and/or column that
    // contains the point.
    val colMax: N = Integral[N].fromLong {
      val colMaxDouble = mapXToGridDouble(subExtent.xmax)

      if (math.abs(colMaxDouble - floorWithTolerance(colMaxDouble)) < GridExtent.epsilon)
        colMaxDouble.toLong - 1L
      else
        colMaxDouble.toLong
    }

    val rowMax: N = Integral[N].fromLong {
      val rowMaxDouble = mapYToGridDouble(subExtent.ymin)

      if (math.abs(rowMaxDouble - floorWithTolerance(rowMaxDouble)) < GridExtent.epsilon)
        rowMaxDouble.toLong - 1L
      else
        rowMaxDouble.toLong
    }

    if (clamp)
      GridBounds(
        colMin = colMin.max(0).min(cols - 1),
        rowMin = rowMin.max(0).min(rows - 1),
        colMax = colMax.max(0).min(cols - 1),
        rowMax = rowMax.max(0).min(rows - 1))
    else
      GridBounds(colMin, rowMin, colMax, rowMax)
  }

  /**
    *  Creates a RasterExtent out of this GridExtent.
    *
    * @note Use with caution: if the number of columns or rows are larger than Int.MaxValue,
    *       this will throw an exception. Also, if columns * rows >
    *       Int.MaxValue, this will create a RasterExtent for a raster
    *       that could not be backed by any of the Array-backed tile
    *       types.
    */
  def toRasterExtent(): RasterExtent = {
    if(cols > Int.MaxValue) {
      throw new GeoAttrsError(s"Cannot convert GridExtent into a RasterExtent: number of columns exceeds maximum integer value ($cols > ${Int.MaxValue})")
    }
    if(rows > Int.MaxValue) {
      throw new GeoAttrsError(s"Cannot convert GridExtent into a RasterExtent: number of rows exceeds maximum integer value ($rows > ${Int.MaxValue})")
    }

    RasterExtent(extent, cellwidth, cellheight, cols.toInt, rows.toInt)
  }

  /**
    * Returns a GridExtent that lines up with this grid's resolution
    * and grid layout.
    *
    * This function will generate an extent that lines up with the grid
    * indicated by the GridExtent, having an origin at the upper-left corner
    * of the extent, and grid cells having the size given by cellSize.
    * The resulting GridExtent, in general, will not be equal to
    * ``targetExtent``, but will have the smallest extent that lines up with
    * the grid and also covers ``targetExtent``.
    */
  def createAlignedGridExtent(targetExtent: Extent): GridExtent[N] = {
    createAlignedGridExtent(targetExtent, extent.northWest)
  }

  /**
    * Returns a GridExtent that with this grid's resolution.
    *
    * This function will generate an extent that lines up with a grid having
    * an origin at the given point and grid cells of the size given by the
    * cellSize of the GridExtent.  The resulting GridExtent, in general, will
    * not be equal to ``targetExtent``, but will have the smallest extent
    * that lines up with the grid and also covers ``targetExtent``.
    */
  def createAlignedGridExtent(targetExtent: Extent, alignmentPoint: Point): GridExtent[N] = {
    def left(reference: Double, actual: Double, unit: Double): Double = reference + math.floor((actual - reference) / unit) * unit
    def right(reference: Double, actual: Double, unit: Double): Double = reference + math.ceil((actual - reference) / unit) * unit

    val xmin = left(alignmentPoint.x, targetExtent.xmin, cellwidth)
    val xmax = right(alignmentPoint.x, targetExtent.xmax, cellwidth)
    val ymin = left(alignmentPoint.y, targetExtent.ymin, cellheight)
    val ymax = right(alignmentPoint.y, targetExtent.ymax, cellheight)
    val alignedExtent = Extent(xmin, ymin, xmax, ymax)
    val cols = math.round(alignedExtent.width / cellwidth)
    val rows = math.round(alignedExtent.height / cellheight)
    val ncols = Integral[N].fromDouble(cols)
    val nrows = Integral[N].fromDouble(rows)

    new GridExtent[N](alignedExtent, cellwidth, cellheight, ncols, nrows)
  }

  /**
    * Tests if the grid is aligned to the extent.
    * This is true when the extent is evenly divided by cellheight and cellwidth.
    */
  def isGridExtentAligned(): Boolean = {
    def isWhole(x: Double) = math.abs(math.round(x) - x) < geotrellis.util.Constants.FLOAT_EPSILON
    isWhole((extent.xmax - extent.xmin) / cellwidth) && isWhole((extent.ymax - extent.ymin) / cellheight)
  }

  /**
    * Returns a RasterExtent that lines up with this RasterExtent's
    * resolution, and grid layout.
    *
    * For example, the resulting RasterExtent will not have the given
    * extent, but will have the smallest extent such that the whole of
    * the given extent is covered, that lines up with the grid.
    */
  def createAlignedRasterExtent(targetExtent: Extent): RasterExtent =
    createAlignedGridExtent(targetExtent).toRasterExtent

  /**
    * This method copies gdalwarp -tap logic:
    *
    * The actual code reference: https://github.com/OSGeo/gdal/blob/v2.3.2/gdal/apps/gdal_rasterize_lib.cpp#L402-L461
    * The actual part with the -tap logic: https://github.com/OSGeo/gdal/blob/v2.3.2/gdal/apps/gdal_rasterize_lib.cpp#L455-L461
    *
    * The initial PR that introduced that feature in GDAL 1.8.0: https://trac.osgeo.org/gdal/attachment/ticket/3772/gdal_tap.patch
    * A discussion thread related to it: https://lists.osgeo.org/pipermail/gdal-dev/2010-October/thread.html#26209
    *
    */
  def alignTargetPixels: GridExtent[N] = {
    val extent = this.extent
    val cellSize @ CellSize(width, height) = this.cellSize

    GridExtent[N](Extent(
      xmin = math.floor(extent.xmin / width) * width,
      ymin = math.floor(extent.ymin / height) * height,
      xmax = math.ceil(extent.xmax / width) * width,
      ymax = math.ceil(extent.ymax / height) * height
    ), cellSize)
  }

  /**
    * Gets the Extent that matches the grid bounds passed in, aligned
    * with this RasterExtent.
    *
    * The 'clamp' parameter determines whether or not to clamp the
    * Extent to the extent of this RasterExtent; defaults to true. If
    * true, the returned extent will be contained by this
    * RasterExtent's extent, if false, the Extent returned can be
    * outside of this RasterExtent's extent.
    *
    * @param  cellBounds  The extent to get the grid bounds for
    * @param  clamp       A boolean which controlls the clamping behvior
    */
  def extentFor(cellBounds: GridBounds[N], clamp: Boolean = true): Extent = {
    val xmin: Double = cellBounds.colMin.toLong * cellwidth + extent.xmin
    val ymax: Double = extent.ymax - (cellBounds.rowMin.toLong * cellheight)
    val xmax: Double = xmin + (cellBounds.width.toLong * cellwidth)
    val ymin: Double = ymax - (cellBounds.height.toLong * cellheight)

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

  /**
  * Adjusts a raster extent so that it can encompass the tile
  * layout.  Will resample the extent, but keep the resolution, and
  * preserve north and west borders
  */
  def adjustTo(tileLayout: TileLayout): GridExtent[N] = {
    val totalCols: Long = tileLayout.tileCols.toLong * tileLayout.layoutCols.toLong
    val totalRows: Long = tileLayout.tileRows.toLong * tileLayout.layoutRows.toLong
    val resampledExtent = Extent(
      xmin = extent.xmin,
      ymin = extent.ymax - (cellheight*totalRows),
      xmax = extent.xmin + (cellwidth*totalCols),
      ymax = extent.ymax)

    new GridExtent[N](resampledExtent, cellwidth, cellheight,
      cols = Integral[N].fromLong(totalCols),
      rows = Integral[N].fromLong(totalRows))
  }

  def canEqual(a: Any) = a.isInstanceOf[GridExtent[_]]

  override def equals(that: Any): Boolean =
    that match {
      case that: GridExtent[_] =>
        that.canEqual(this) &&
        that.extent == this.extent &&
        that.cellSize == this.cellSize &&
        that.cols == this.cols &&
        that.rows == this.rows
      case _ => false
  }

  override def hashCode(): Int =
    (((31 + (if (extent == null) 0 else extent.hashCode)) * 31 + cellheight.toInt) * 31 + cellwidth.toInt)

  def toGridType[M: Integral]: GridExtent[M] = {
    new GridExtent[M](extent, cellwidth, cellheight, Integral[N].toType[M](cols), Integral[N].toType[M](rows))
  }

  override def toString: String =
    s"""GridExtent($extent,$cellSize,${cols}x${rows})"""
}


object GridExtent {
  final val epsilon = 0.0000001

  def apply[N: Integral](extent: Extent, cellSize: CellSize): GridExtent[N] = {
    new GridExtent[N](extent, cellSize)
  }

  def apply[N: Integral](extent: Extent, cols: N, rows: N): GridExtent[N] = {
    val cw = extent.width / cols.toDouble
    val ch = extent.height / rows.toDouble
    new GridExtent[N](extent, cw, ch, cols, rows)
  }

  def apply[N: Integral](extent: Extent, grid: Grid[N]): GridExtent[N] = {
    val cw = extent.width / grid.cols.toDouble
    val ch = extent.height / grid.rows.toDouble
    new GridExtent[N](extent, cw, ch, grid.cols, grid.rows)
  }

  /** RasterSource interface reads GridBounds[Long] but GridBounds[Int] abounds.
   * Implicit conversions are evil, but this one is always safe and saves typing.
   */
  implicit def gridBoundsIntToLong(bounds: GridBounds[Int]): GridBounds[Long] = bounds.toGridType[Long]

  /**
    * The same logic is used in QGIS: https://github.com/qgis/QGIS/blob/607664c5a6b47c559ed39892e736322b64b3faa4/src/analysis/raster/qgsalignraster.cpp#L38
    * The search query: https://github.com/qgis/QGIS/search?p=2&q=floor&type=&utf8=%E2%9C%93
    *
    * GDAL uses smth like that, however it was a bit hard to track it down:
    * https://github.com/OSGeo/gdal/blob/7601a637dfd204948d00f4691c08f02eb7584de5/gdal/frmts/vrt/vrtsources.cpp#L215
    * */
  def floorWithTolerance(value: Double): Double = {
    val roundedValue = math.round(value)
    if (math.abs(value - roundedValue) < GridExtent.epsilon) roundedValue
    else math.floor(value)
  }

  implicit class gridExtentMethods[N: Integral](self: GridExtent[N]) {
    def reproject(src: CRS, dest: CRS, resampleTarget: ResampleTarget = DefaultTarget): GridExtent[N] =
      if (src == dest) self else ReprojectRasterExtent(self, Transform(src, dest), resampleTarget)
  }
}
