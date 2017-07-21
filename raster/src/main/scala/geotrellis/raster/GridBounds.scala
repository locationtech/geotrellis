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

  @deprecated("This will return a `Long` in 2.0. Until then, sizeLong may be more accurate.", "1.2")
  def size: Int = width * height

  // TODO Mark for deprecation in 3.0 when 2.0 comes out!
  def sizeLong: Long = width.toLong * width.toLong

  def isEmpty = sizeLong == 0

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

  @deprecated("Use `coordsIter` instead.", "1.2")
  def coords: Array[(Int, Int)] = {
    val arr = Array.ofDim[(Int, Int)](width*height)
    cfor(0)(_ < height, _ + 1) { row =>
      cfor(0)(_ < width, _ + 1) { col =>
        arr(row * width + col) =
          (col + colMin, row + rowMin)
      }
    }
    arr
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
}
