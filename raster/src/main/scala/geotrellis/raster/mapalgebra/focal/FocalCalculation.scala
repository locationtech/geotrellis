/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.TargetCell.TargetCell

/**
 * Declares that implementers have a result
 */
trait Resulting[T] {
  def result: T
}

object TargetCell extends Enumeration {
  type TargetCell = Value
  val NoData, Data, All = Value
}

/**
 * A calculation that a FocalStrategy uses to complete
 * a focal operation.
 */
abstract class FocalCalculation[T](val tile: Tile, n: Neighborhood, target : TargetCell, analysisArea: Option[GridBounds])
  extends Resulting[T]
{
  val bounds: GridBounds = analysisArea.getOrElse(GridBounds(tile))

  def execute(): T

  def setValidResult(x: Int, y: Int, focusCol: Int, focusRow: Int, value: Int) {
    val cellValue = tile.get(focusCol, focusRow)

    set(x, y, {if (validTargetCell(cellValue)) value else cellValue})
  }

  def setValidResult(cursor: Cursor, value: Int) {
    val (focusCol, focusRow) = focusPosition(cursor)

    setValidResult(cursor.col, cursor.row, focusCol, focusRow, value)
  }


  // Some focal ops may produce double results from int input, i.e. mean
  def setValidDoubleResultFromInt(x: Int, y: Int, focusCol: Int, focusRow: Int, value: Double) {
    val cellValue = tile.get(focusCol, focusRow)

    setDouble(x, y, {if (validTargetCell(cellValue)) value else cellValue})
  }

  def setValidDoubleResultFromInt(cursor: Cursor, value: Double) {
    val (focusCol, focusRow) = focusPosition(cursor)

    setValidDoubleResultFromInt(cursor.col, cursor.row, focusCol, focusRow, value)
  }

  def setValidDoubleResult(x: Int, y: Int, focusCol: Int, focusRow: Int, value: Double) {
    val cellValue = tile.get(focusCol, focusRow)

    setDouble(x, y, {if (validTargetCell(cellValue)) value else cellValue})
  }

  def setValidDoubleResult(cursor: Cursor, value: Double) {
    val (focusCol, focusRow) = focusPosition(cursor)

    setValidDoubleResult(cursor.col, cursor.row, focusCol, focusRow, value)
  }

  def focusPosition(cursor: Cursor) =
    (cursor.analysisOffsetCols + cursor.col, cursor.analysisOffsetRows + cursor.row)


  def set(x: Int, y: Int, value: Int)

  def setDouble(x: Int, y: Int, value: Double)

  def validTargetCell(value: Int) =
    target == TargetCell.All || {if (isData(value)) TargetCell.Data else TargetCell.NoData} == target

  def validTargetCell(value: Double) =
    target == TargetCell.All || {if (isData(value)) TargetCell.Data else TargetCell.NoData} == target
}

/**
 * A focal calculation that uses the Cursor focal strategy.
 */
abstract class CursorCalculation[T](tile: Tile, n: Neighborhood, target: TargetCell, val analysisArea: Option[GridBounds])
  extends FocalCalculation[T](tile, n, target, analysisArea)
{
  def traversalStrategy = TraversalStrategy.DEFAULT

  def execute(): T = {
    val cursor = Cursor(tile, n, bounds)
    CursorStrategy.execute(cursor, { () => calc(tile, cursor) }, bounds, traversalStrategy)
    result
  }

  def calc(tile: Tile, cursor: Cursor): Unit
}

/**
 * A focal calculation that uses the Cursor focal strategy.
 */
abstract class KernelCalculation[T](tile: Tile, kernel: Kernel, target: TargetCell, val analysisArea: Option[GridBounds])
    extends FocalCalculation[T](tile, kernel, target, analysisArea)
{
  // Benchmarking has declared ScanLineTraversalStrategy the unclear winner as a default (based on Convolve).
  def traversalStrategy = ScanLineTraversalStrategy

  def execute(): T = {
    val cursor = new KernelCursor(tile, kernel, bounds)
    CursorStrategy.execute(cursor, { () => calc(tile, cursor) }, bounds, traversalStrategy)
    result
  }

  def calc(tile: Tile, kernel: KernelCursor): Unit
}

/**
 * A focal calculation that uses the Cellwise focal strategy
 */
abstract class CellwiseCalculation[T] (tile: Tile, n: Neighborhood, target: TargetCell, analysisArea: Option[GridBounds])
  extends FocalCalculation[T](tile, n, target, analysisArea)
{
  def traversalStrategy: Option[TraversalStrategy] = None

  def execute(): T = n match {
    case s: Square =>
      CellwiseStrategy.execute(tile, s, this, target, bounds)
      result
    case _ => sys.error("Cannot use cellwise calculation with this traversal strategy.")
  }

  def add(tile: Tile, x: Int, y: Int)
  def remove(tile: Tile, x: Int, y: Int)
  def reset(): Unit
  def setValue(x: Int, y: Int, focusCol: Int, focusRow: Int)
}

/*
 * Mixin's that define common raster-result functionality
 * for FocalCalculations.
 * Access the resulting raster's array tile through the
 * 'resultTile' member.
 */

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[BitArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait BitArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[BitArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile: BitArrayTile = BitArrayTile.empty(cols, rows)

  def result = resultTile
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[ByteArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait ByteArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[ByteArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile: ByteArrayTile = ByteArrayTile.empty(cols, rows)

  def result = resultTile

  def set(x: Int, y: Int, value: Int) =
    result.set(x, y, value)

  def setDouble(x: Int, y: Int, value: Double) =
    result.setDouble(x, y, value)
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[ShortArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait ShortArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[ShortArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile: ShortArrayTile = ShortArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def result = resultTile

  def set(x: Int, y: Int, value: Int) =
    result.set(x, y, value)

  def setDouble(x: Int, y: Int, value: Double) =
    result.setDouble(x, y, value)
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[IntArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait IntArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[IntArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile = IntArrayTile.empty(cols, rows)

  def result = resultTile

  def set(x: Int, y: Int, value: Int) =
    result.set(x, y, value)

  def setDouble(x: Int, y: Int, value: Double) =
    result.setDouble(x, y, value)
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[FloatArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait FloatArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[FloatArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile: FloatArrayTile = FloatArrayTile.empty(cols, rows)

  def result = resultTile

  def set(x: Int, y: Int, value: Int) =
    result.set(x, y, value)

  def setDouble(x: Int, y: Int, value: Double) =
    result.setDouble(x, y, value)
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[DoubleArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait DoubleArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[DoubleArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile = DoubleArrayTile.empty(cols, rows)

  def result = resultTile

  def set(x: Int, y: Int, value: Int) =
    result.set(x, y, value)

  def setDouble(x: Int, y: Int, value: Double) =
    result.setDouble(x, y, value)
}

trait ArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  def resultCellType: DataType with NoDataHandling = tile.cellType
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile: MutableArrayTile = ArrayTile.empty(resultCellType, cols, rows)

  def result = resultTile

  def set(x: Int, y: Int, value: Int) =
    result.set(x, y, value)

  def setDouble(x: Int, y: Int, value: Double) =
    result.setDouble(x, y, value)
}
