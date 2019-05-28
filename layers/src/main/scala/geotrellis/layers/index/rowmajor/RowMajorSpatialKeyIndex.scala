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

package geotrellis.layers.index.rowmajor

import geotrellis.tiling.{SpatialKey, KeyBounds}
import geotrellis.layers._
import geotrellis.layers.index.KeyIndex

import spire.syntax.cfor._

/** Represents a row major ordering for SpatialKey */
class RowMajorSpatialKeyIndex(val keyBounds: KeyBounds[SpatialKey]) extends KeyIndex[SpatialKey] {
  val minCol = keyBounds.minKey.col
  val minRow = keyBounds.minKey.row
  val layoutCols = keyBounds.maxKey.col - keyBounds.minKey.col + 1

  def toIndex(key: SpatialKey): BigInt =
    toIndex(key.col, key.row)

  def toIndex(col: Int, row: Int): BigInt =
    BigInt(layoutCols * (row - minRow) + (col - minCol))

  def indexRanges(keyRange: (SpatialKey, SpatialKey)): Seq[(BigInt, BigInt)] = {
    val SpatialKey(colMin, rowMin) = keyRange._1
    val SpatialKey(colMax, rowMax) = keyRange._2

    val cols = colMax - colMin + 1
    val rows = rowMax - rowMin

    val result = Array.ofDim[(BigInt, BigInt)](rowMax - rowMin + 1)

    cfor(0)(_ <= rows, _ + 1) { i =>
      val row = rowMin + i
      val min = toIndex(colMin, row)
      result(i) = (min, min + cols - 1)
    }
    result
  }
}
