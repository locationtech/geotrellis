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

import geotrellis.vector.{Extent, Point}

import scala.math.{min, max, ceil}
import spire.math.{Integral, NumberTag}
import spire.implicits._

/**
  * Represents an abstract grid over geographic extent.
  * Critically while the number of cell rows and columns is implied by the constructor arguments,
  * they are intentionally not expressed to avoid Int overflow for large grids.
  */
class GridExtent[@specialized(Short, Int, Long) N: Integral](
  val extent: Extent,
  val cellwidth: Double,
  val cellheight: Double,
  cols: N,
  rows: N
) extends Serializable {

  def this(extent: Extent, cellSize: CellSize) = {
    this(extent, cellSize.width, cellSize.height,
      cols = integralFromLong(math.round(extent.width / cellSize.width).toLong),
      rows = integralFromLong(math.round(extent.height / cellSize.height).toLong))
  }

  /** Convert map coordinate x to grid coordinate column. */
  final def mapXToGridDouble(x: Double): Double = (x - extent.xmin) / cellwidth

  /** Convert map coordinate y to grid coordinate row. */
  final def mapYToGridDouble(y: Double): Double = (extent.ymax - y ) / cellheight

  /** Convert map coordinate x to grid coordinate column. */
  final def mapXToGrid(x: Double): N = integralFromLong[N](mapXToGridDouble(x).toLong)

  /** Convert map coordinate y to grid coordinate row. */
  final def mapYToGrid(y: Double): N = integralFromLong[N](mapYToGridDouble(y).toLong)

  // TODO: move this into constructor
  def cellSize = CellSize(cellwidth, cellheight)

  /**
    * Gets the GridBounds aligned with this RasterExtent that is the
    * smallest subgrid of containing all points within the extent. The
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
    val colMax: N = integralFromLong[N]{
      val colMaxDouble = mapXToGridDouble(subExtent.xmax)

      if (math.abs(colMaxDouble - math.floor(colMaxDouble)) < RasterExtent.epsilon)
        colMaxDouble.toLong - 1L
      else
        colMaxDouble.toLong
    }

    val rowMax: N = integralFromLong[N]{
      val rowMaxDouble = mapYToGridDouble(subExtent.ymin)

      if (math.abs(rowMaxDouble - math.floor(rowMaxDouble)) < RasterExtent.epsilon)
        rowMaxDouble.toLong - 1L
      else
        rowMaxDouble.toLong
    }

    if (clamp)
      GridBounds(colMin, rowMin, colMax.min(cols - 1), rowMax.min(rows - 1))
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
    val targetCols = math.max(1L, math.round(extent.width / cellwidth).toLong)
    val targetRows = math.max(1L, math.round(extent.height / cellheight).toLong)
    if(targetCols > Int.MaxValue) {
      throw new GeoAttrsError(s"Cannot convert GridExtent into a RasterExtent: number of columns exceeds maximum integer value ($targetCols > ${Int.MaxValue})")
    }
    if(targetRows > Int.MaxValue) {
      throw new GeoAttrsError(s"Cannot convert GridExtent into a RasterExtent: number of rows exceeds maximum integer value ($targetRows > ${Int.MaxValue})")
    }

    RasterExtent(extent, cellwidth, cellheight, targetCols.toInt, targetRows.toInt)
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
    val cols = math.round(extent.width / cellwidth).toLong
    val rows = math.round(extent.height / cellheight).toLong
    val ncols = integralFromLong(cols)
    val nrows = integralFromLong(rows)

    new GridExtent[N](Extent(xmin, ymin, xmax, ymax), cellwidth, cellheight, ncols, nrows)
  }

  /**
    * Tests if the grid is aligned to the extent.
    * This is true when the extent is evenly divided by cellheight and cellwidth.
    */
  def isGridExtentAligned(): Boolean = {
    def isWhole(x: Double) = math.abs(math.floor(x) - x) < geotrellis.util.Constants.DOUBLE_EPSILON
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

  override def equals(o: Any): Boolean = o match {
    case other: GridExtent[_] =>
      // TODO: check if cols/rows are same type
      other.extent == extent && other.cellheight == cellheight && other.cellwidth == cellwidth
    case _ =>
      false
  }

  override def hashCode(): Int =
    (((31 + (if (extent == null) 0 else extent.hashCode)) * 31 + cellheight.toInt) * 31 + cellwidth.toInt)

  def withGridType[M: Integral]: GridExtent[M] = {
    import spire.implicits._
    val ncols = integralFromLong[M](cols.toLong)
    val nrows = integralFromLong[M](cols.toLong)
    new GridExtent[M](extent, cellwidth, cellheight, ncols, nrows)
  }
}


object GridExtent {
  def apply(extent: Extent, cellSize: CellSize): GridExtent[Long] =
    new GridExtent[Long](extent, cellSize)

  def apply(extent: Extent, cellwidth: Double, cellheight: Double): GridExtent[Long] =
    new GridExtent[Long](extent, CellSize(cellwidth, cellheight))

  def unapply[N](ge: GridExtent[N]): Option[(Extent, Double, Double)] =
    if (ge != null)
      Some((ge.extent, ge.cellwidth, ge.cellheight))
    else
      None
}
