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

import scala.collection.mutable
import spire.syntax.cfor._

/**
  * The companion object for the [[GridBounds]] type.
  */
object GridBounds {
  /**
    * Given a [[CellGrid]], produce the corresponding [[GridBounds]].
    *
    * @param  r  The given CellGrid
    */
  def apply(r: CellGrid): GridBounds =
    GridBounds(0, 0, r.cols-1, r.rows-1)

  /**
    * Given a sequence of keys, return a [[GridBounds]] of minimal
    * size which covers them.
    *
    * @param  keys  The sequence of keys to cover
    */
  def envelope(keys: Iterable[Product2[Int, Int]]): GridBounds = {
    var colMin = Integer.MAX_VALUE
    var colMax = Integer.MIN_VALUE
    var rowMin = Integer.MAX_VALUE
    var rowMax = Integer.MIN_VALUE

    for (key <- keys) {
      val col = key._1
      val row = key._2
      if (col < colMin) colMin = col
      if (col > colMax) colMax = col
      if (row < rowMin) rowMin = row
      if (row > rowMax) rowMax = row
    }
    GridBounds(colMin, rowMin, colMax, rowMax)
  }

  /**
    * Creates a sequence of distinct [[GridBounds]] out of a set of
    * potentially overlapping grid bounds.
    *
    * @param  gridBounds  A traversable collection of GridBounds
    */
  def distinct(gridBounds: Traversable[GridBounds]): Seq[GridBounds] =
    gridBounds.foldLeft(Seq[GridBounds]()) { (acc, bounds) =>
      acc ++ acc.foldLeft(Seq(bounds)) { (cuts, bounds) =>
        cuts.flatMap(_ - bounds)
      }
    }
}

/**
  * Represents grid coordinates of a subsection of a RasterExtent.
  * These coordinates are inclusive.
  */
case class GridBounds(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int) {
  def width = colMax - colMin + 1
  def height = rowMax - rowMin + 1

  def size: Long = width.toLong * height.toLong

  @deprecated("Use `size` instead.", "2.0")
  def sizeLong: Long = width.toLong * height.toLong

  def isEmpty = size == 0

  /**
    * Return true if the present [[GridBounds]] contains the position
    * pointed to by the given column and row, otherwise false.
    *
    * @param  col  The column
    * @param  row  The row
    */
  def contains(col: Int, row: Int): Boolean =
    (colMin <= col && col <= colMax) &&
    (rowMin <= row && row <= rowMax)

  /**
    * Returns true if the present [[GridBounds]] and the given one
    * intersect (including their boundaries), otherwise returns false.
    *
    * @param  other  The other GridBounds
    */
  def intersects(other: GridBounds): Boolean =
    !(colMax < other.colMin || other.colMax < colMin) &&
    !(rowMax < other.rowMin || other.rowMax < rowMin)

  /**
   * Creates a new [[GridBounds]] using a buffer around this
   * GridBounds.
   *
   * @note This will not buffer past 0 regardless of how much the buffer
   *       falls below it.
   *
   * @param bufferSize The amount this GridBounds should be buffered by.
   */
  def buffer(bufferSize: Int): GridBounds =
    buffer(bufferSize, bufferSize)

  /**
   * Creates a new [[GridBounds]] using a buffer around this
   * GridBounds.
   *
   * @note This will not buffer past 0 regardless of how much the buffer
   *       falls below it.
   *
   * @param colBuffer The amount the cols within this GridBounds should be buffered.
   * @param rowBuffer The amount the rows within this GridBounds should be buffered.
   * @param clamp     Determines whether or not to clamp the GridBounds to the grid
   *                  such that it no value will be under 0; defaults to true. If false,
   *                  then the resulting GridBounds can contain negative values outside
   *                  of the grid boundaries.
   */
  def buffer(colBuffer: Int, rowBuffer: Int, clamp: Boolean = true): GridBounds =
    GridBounds(
      if (clamp) math.max(colMin - colBuffer, 0) else colMin - colBuffer,
      if (clamp) math.max(rowMin - rowBuffer, 0) else rowMin - rowBuffer,
      colMax + colBuffer,
      rowMax + rowBuffer
    )

  /**
   * Offsets this [[GridBounds]] to a new location relative to its current
   * position
   *
   * @param boundsOffset The amount the GridBounds should be shifted.
   */
  def offset(boundsOffset: Int): GridBounds =
    offset(boundsOffset, boundsOffset)

  /**
   * Offsets this [[GridBounds]] to a new location relative to its current
   * position
   *
   * @param colOffset The amount the cols should be shifted.
   * @param rowOffset The amount the rows should be shifted.
   */
  def offset(colOffset: Int, rowOffset: Int): GridBounds =
    GridBounds(
      colMin + colOffset,
      rowMin + rowOffset,
      colMax + colOffset,
      rowMax + rowOffset
    )

  /**
    * Another name for the 'minus' method.
    *
    * @param  other  The other GridBounds
    */
  def -(other: GridBounds): Seq[GridBounds] = minus(other)

  /**
    * Returns the difference of the present [[GridBounds]] and the
    * given one.  This returns a sequence, because the difference may
    * consist of more than one GridBounds.
    *
    * @param  other  The other GridBounds
    */
  def minus(other: GridBounds): Seq[GridBounds] =
    if(!intersects(other)) {
      Seq(this)
    } else {
      val overlapColMin =
        if(colMin < other.colMin) other.colMin
        else colMin

      val overlapColMax =
        if(colMax < other.colMax) colMax
        else other.colMax

      val overlapRowMin =
        if(rowMin < other.rowMin) other.rowMin
        else rowMin

      val overlapRowMax =
        if(rowMax < other.rowMax) rowMax
        else other.rowMax

      val result = mutable.ListBuffer[GridBounds]()
      // Left cut
      if(colMin < overlapColMin) {
        result += GridBounds(colMin, rowMin, overlapColMin - 1, rowMax)
      }

      // Right cut
      if(overlapColMax < colMax) {
        result += GridBounds(overlapColMax + 1, rowMin, colMax, rowMax)
      }

      // Top cut
      if(rowMin < overlapRowMin) {
        result += GridBounds(overlapColMin, rowMin, overlapColMax, overlapRowMin - 1)
      }

      // Bottom cut
      if(overlapRowMax < rowMax) {
        result += GridBounds(overlapColMin, overlapRowMax + 1, overlapColMax, rowMax)
      }
      result
    }

  /**
    * Return the coordinates covered by the present [[GridBounds]].
    */
  def coordsIter: Iterator[(Int, Int)] = for {
    row <- Iterator.range(0, height)
    col <- Iterator.range(0, width)
  } yield (col + colMin, row + rowMin)

  /**
    * Return the intersection of the present [[GridBounds]] and the
    * given [[CellGrid]].
    *
    * @param  cellGrid  The cellGrid to intersect with
    */
  def intersection(cellGrid: CellGrid): Option[GridBounds] =
    intersection(GridBounds(cellGrid))

  /**
    * Return the intersection of the present [[GridBounds]] and the
    * given [[GridBounds]].
    *
    * @param  other  The other GridBounds
    */
  def intersection(other: GridBounds): Option[GridBounds] =
    if(!intersects(other)) {
      None
    } else {
      Some(
        GridBounds(
          math.max(colMin, other.colMin),
          math.max(rowMin, other.rowMin),
          math.min(colMax, other.colMax),
          math.min(rowMax, other.rowMax)
        )
      )
    }

  /** Return the union of GridBounds. */
  def combine(other: GridBounds): GridBounds =
    GridBounds(
      colMin = math.min(this.colMin, other.colMin),
      rowMin = math.min(this.rowMin, other.rowMin),
      colMax = math.max(this.colMax, other.colMax),
      rowMax = math.max(this.rowMax, other.rowMax)
    )

  /** Empty gridbounds contain nothing, though non empty gridbounds contains iteslf */
  def contains(other: GridBounds): Boolean =
    if(colMin == 0 && colMax == 0 && rowMin == 0 && rowMax == 0) false
    else
      other.colMin >= colMin &&
      other.rowMin >= rowMin &&
      other.colMax <= colMax &&
      other.rowMax <= rowMax

  /** Split into windows, covering original GridBounds */
  def split(cols: Int, rows: Int): Iterator[GridBounds] = {
    for {
      windowRowMin <- Iterator.range(start = rowMin, end = rowMax + 1, step = rows)
      windowColMin <- Iterator.range(start = colMin, end = colMax + 1, step = cols)
    } yield {
      GridBounds(
        colMin = windowColMin,
        rowMin = windowRowMin,
        colMax = math.min(windowColMin + cols - 1, colMax),
        rowMax = math.min(windowRowMin + rows - 1, rowMax)
      )
    }
  }
}
