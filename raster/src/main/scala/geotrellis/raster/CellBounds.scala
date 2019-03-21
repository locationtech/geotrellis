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

package geotrellis.raster

import scala.collection.mutable
import spire.math._
import spire.implicits._

case class CellBounds[A: Integral](colMin: A, rowMin: A, colMax: A, rowMax: A) {
  def width: A = colMax - colMin + Integral[A].one
  def height: A = rowMax - rowMin + Integral[A].one

  def size: A = width * height

  def isEmpty: Boolean = size == 0

  /**
    * Return true if the present [[CellBounds]] contains the position
    * pointed to by the given column and row, otherwise false.
    *
    * @param  col  The column
    * @param  row  The row
    */
  def contains(col: A, row: A): Boolean =
    (colMin <= col && col <= colMax) &&
      (rowMin <= row && row <= rowMax)

  /**
    * Returns true if the present [[CellBounds]] and the given one
    * intersect (including their boundaries), otherwise returns false.
    *
    * @param  other  The other CellBounds
    */
  def intersects(other: CellBounds[A]): Boolean =
    !(colMax < other.colMin || other.colMax < colMin) &&
      !(rowMax < other.rowMin || other.rowMax < rowMin)

  /**
    * Creates a new [[CellBounds]] using a buffer around this
    * CellBounds.
    *
    * @note This will not buffer past 0 regardless of how much the buffer
    *       falls below it.
    *
    * @param bufferSize The amount this CellBounds should be buffered by.
    */
  def buffer(bufferSize: Int): CellBounds[A] =
    buffer(bufferSize, bufferSize)

  /**
    * Creates a new [[CellBounds]] using a buffer around this
    * CellBounds.
    *
    * @note This will not buffer past 0 regardless of how much the buffer
    *       falls below it.
    *
    * @param colBuffer The amount the cols within this CellBounds should be buffered.
    * @param rowBuffer The amount the rows within this CellBounds should be buffered.
    * @param clamp     Determines whether or not to clamp the CellBounds to the grid
    *                  such that it no value will be under 0; defaults to true. If false,
    *                  then the resulting CellBounds can contain negative values outside
    *                  of the grid boundaries.
    */
  def buffer(colBuffer: A, rowBuffer: A, clamp: Boolean = true): CellBounds[A] =
    CellBounds(
      if (clamp) Integral[A].max(colMin - colBuffer, 0) else colMin - colBuffer,
      if (clamp) Integral[A].max(rowMin - rowBuffer, 0) else rowMin - rowBuffer,
      colMax + colBuffer,
      rowMax + rowBuffer
    )

  /**
    * Offsets this [[CellBounds]] to a new location relative to its current
    * position
    *
    * @param boundsOffset The amount the CellBounds should be shifted.
    */
  def offset(boundsOffset: A): CellBounds[A] =
    offset(boundsOffset, boundsOffset)

  /**
    * Offsets this [[CellBounds]] to a new location relative to its current
    * position
    *
    * @param colOffset The amount the cols should be shifted.
    * @param rowOffset The amount the rows should be shifted.
    */
  def offset(colOffset: A, rowOffset: A): CellBounds[A] =
    CellBounds(
      colMin + colOffset,
      rowMin + rowOffset,
      colMax + colOffset,
      rowMax + rowOffset
    )

  /**
    * Another name for the 'minus' method.
    *
    * @param  other  The other CellBounds
    */
  def -(other: CellBounds[A]): Seq[CellBounds[A]] = minus(other)

  /**
    * Returns the difference of the present [[CellBounds]] and the
    * given one.  This returns a sequence, because the difference may
    * consist of more than one CellBounds.
    *
    * @param  other  The other CellBounds
    */
  def minus(other: CellBounds[A]): Seq[CellBounds[A]] =
    if(!intersects(other)) {
      Seq(this)
    } else {
      val overlapColMin =
        if(colMin < other.colMin) other.colMin else colMin

      val overlapColMax =
        if(colMax < other.colMax) colMax else other.colMax

      val overlapRowMin =
        if(rowMin < other.rowMin) other.rowMin else rowMin

      val overlapRowMax =
        if(rowMax < other.rowMax) rowMax else other.rowMax

      val result = mutable.ListBuffer[CellBounds[A]]()
      // Left cut
      if(colMin < overlapColMin) {
        result += CellBounds(colMin, rowMin, overlapColMin - 1, rowMax)
      }

      // Right cut
      if(overlapColMax < colMax) {
        result += CellBounds(overlapColMax + 1, rowMin, colMax, rowMax)
      }

      // Top cut
      if(rowMin < overlapRowMin) {
        result += CellBounds(overlapColMin, rowMin, overlapColMax, overlapRowMin - 1)
      }

      // Bottom cut
      if(overlapRowMax < rowMax) {
        result += CellBounds(overlapColMin, overlapRowMax + 1, overlapColMax, rowMax)
      }
      result
    }


  /**
    * Return the intersection of the present [[CellBounds]] and the
    * given [[CellBounds]].
    *
    * @param  other  The other CellBounds
    */
  def intersection(other: CellBounds[A]): Option[CellBounds[A]] =
    if(!intersects(other)) {
      None
    } else {
      Some(
        CellBounds(
          Integral[A].max(colMin, other.colMin),
          Integral[A].max(rowMin, other.rowMin),
          Integral[A].min(colMax, other.colMax),
          Integral[A].min(rowMax, other.rowMax)
        )
      )
    }

  /** Return the union of CellBounds. */
  def combine(other: CellBounds[A]): CellBounds[A] =
    CellBounds(
      colMin = Integral[A].min(this.colMin, other.colMin),
      rowMin = Integral[A].min(this.rowMin, other.rowMin),
      colMax = Integral[A].max(this.colMax, other.colMax),
      rowMax = Integral[A].max(this.rowMax, other.rowMax)
    )

  /** Empty CellBounds contain nothing, though non empty CellBounds contains iteslf */
  def contains(other: CellBounds[A]): Boolean =
    if (colMin == 0 && colMax == 0 && rowMin == 0 && rowMax == 0) {
      false // empty bounds can not contain anything
    } else {
      other.colMin >= colMin &&
        other.rowMin >= rowMin &&
        other.colMax <= colMax &&
        other.rowMax <= rowMax
    }

  /** Split into windows, covering original CellBounds */
  def split(cols: A, rows: A)(implicit ev: NumberTag[A]): Iterator[CellBounds[A]] = {
    for {
      windowRowMin <- Interval.closed(rowMin, rowMax).iterator(rows)
      windowColMin <- Interval.closed(colMin, colMax).iterator(cols)
    } yield {
      CellBounds(
        colMin = windowColMin,
        rowMin = windowRowMin,
        colMax = Integral[A].min(windowColMin + cols - 1, colMax),
        rowMax = Integral[A].min(windowRowMin + rows - 1, rowMax)
      )
    }
  }

  def toInt: CellBounds[Int] = {
    require(colMin <= Int.MaxValue && colMax <= Int.MaxValue
      && rowMin <= Int.MaxValue && rowMax <= Int.MaxValue,
      s"$this exceeds Int.MaxValue")
    CellBounds(colMin.toInt, rowMin.toInt, colMax.toInt, rowMax.toInt)
  }

  def toLong: CellBounds[Long] = {
    CellBounds(colMin.toLong, rowMin.toLong, colMax.toLong, rowMax.toLong)
  }

}

object CellBounds {

  /** Utility method that will type check if value of a long can be represented as instance of Integral */
  def integralFromLongOption[@specialized(Int, Long) N: Integral](x: Long)(implicit integral: Integral[N]): Option[N] = {
    val n: N = integral.fromLong(x)
    if (integral.toLong(n) == x) Some(n) else None
  }

  def integralFromLong[@specialized(Int, Long) N: Integral](x: Long)(implicit integral: Integral[N]): N = {
    val n = integral.fromInt(x.toInt)
    if (integral.toLong(n) == x) n
    else throw new IllegalArgumentException(s"Value $x can not be represented by ${integral.getClass.getSimpleName}")
  }
}