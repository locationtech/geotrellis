package geotrellis.spark.io.index.rowmajor

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex

import spire.syntax.cfor._

/** Represents a row major ordering for GridKey */
class RowMajorGridKeyIndex(val keyBounds: KeyBounds[GridKey]) extends KeyIndex[GridKey] {
  val minCol = keyBounds.minKey.col
  val minRow = keyBounds.minKey.row
  val layoutCols = keyBounds.maxKey.col - keyBounds.minKey.col + 1

  def toIndex(key: GridKey): Long =
    toIndex(key.col, key.row)

  def toIndex(col: Int, row: Int): Long =
    (layoutCols * (row - minRow) + (col - minCol)).toLong

  def indexRanges(keyRange: (GridKey, GridKey)): Seq[(Long, Long)] = {
    val GridKey(colMin, rowMin) = keyRange._1
    val GridKey(colMax, rowMax) = keyRange._2

    val cols = colMax - colMin + 1
    val rows = rowMax - rowMin

    val result = Array.ofDim[(Long, Long)](rowMax - rowMin + 1)

    cfor(0)(_ <= rows, _ + 1) { i =>
      val row = rowMin + i
      val min = toIndex(colMin, row)
      result(i) = (min, min + cols - 1)
    }
    result
  }
}
