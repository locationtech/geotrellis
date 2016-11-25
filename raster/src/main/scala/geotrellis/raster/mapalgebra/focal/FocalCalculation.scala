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

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._

trait Resulting[T] {
  /** Copies original value to result, (focusCol: Int, focusRow: Int, resultCol: Int, rowRow: Int) => Unit */
  val copyOriginalValue: (Int, Int, Int, Int) => Unit
  def result: T
}

sealed trait TargetCell extends Serializable

object TargetCell {
  object NoData extends TargetCell
  object Data extends TargetCell
  object All extends TargetCell
}

/**
 * A calculation that a FocalStrategy uses to complete
 * a focal operation.
 */
abstract class FocalCalculation[T](
    val r: Tile, n: Neighborhood, analysisArea: Option[GridBounds], val target: TargetCell)
  extends Resulting[T]
{
  val bounds: GridBounds = analysisArea.getOrElse(GridBounds(r))

  def execute(): T
}

/**
 * A focal calculation that uses the Cursor focal strategy.
 */
abstract class CursorCalculation[T](tile: Tile, n: Neighborhood, val analysisArea: Option[GridBounds], target: TargetCell)
  extends FocalCalculation[T](tile, n, analysisArea, target)
{
  def traversalStrategy = TraversalStrategy.DEFAULT

  def execute(): T = {
    val cursor = Cursor(tile, n, bounds)
    val calcFunc: () => Unit =
      target match {
        case TargetCell.All =>
          { () => calc(tile, cursor) }
        case TargetCell.Data =>
          { () =>
            calc(tile, cursor)
            if(!isData(r.get(cursor.focusCol, cursor.focusRow))){
              copyOriginalValue(cursor.focusCol, cursor.focusRow, cursor.col, cursor.row)
            }
          }
        case TargetCell.NoData =>
          { () =>
            calc(tile, cursor)
            if(!isNoData(r.get(cursor.focusCol, cursor.focusRow))) {
              copyOriginalValue(cursor.focusCol, cursor.focusRow, cursor.col, cursor.row)
            }
          }
      }

    CursorStrategy.execute(cursor, calcFunc, bounds, traversalStrategy)
    result
  }

  def calc(tile: Tile, cursor: Cursor): Unit
}

/**
 * A focal calculation that uses the Cursor focal strategy.
 */
abstract class KernelCalculation[T](tile: Tile, kernel: Kernel, val analysisArea: Option[GridBounds], target: TargetCell)
    extends FocalCalculation[T](tile, kernel, analysisArea, target)
{
  // Benchmarking has declared ScanLineTraversalStrategy the unclear winner as a default (based on Convolve).
  def traversalStrategy = ScanLineTraversalStrategy

  def execute(): T = {
    val cursor = new KernelCursor(tile, kernel, bounds)
    val calcFunc: () => Unit =
      target match {
        case TargetCell.All =>
          { () => calc(tile, cursor) }
        case TargetCell.Data =>
          { () =>
            calc(tile, cursor)
            if(!isData(r.get(cursor.focusCol, cursor.focusRow))) {
              copyOriginalValue(cursor.focusCol, cursor.focusRow, cursor.col, cursor.row)
            }
          }
        case TargetCell.NoData =>
          { () =>
            calc(tile, cursor)
            if(!isNoData(r.get(cursor.focusCol, cursor.focusRow))) {
              copyOriginalValue(cursor.focusCol, cursor.focusRow, cursor.col, cursor.row)
            }
          }
      }

    CursorStrategy.execute(cursor, calcFunc, bounds, traversalStrategy)
    result
  }

  def calc(r: Tile, kernel: KernelCursor): Unit
}

/**
 * A focal calculation that uses the Cellwise focal strategy
 */
abstract class CellwiseCalculation[T] (
    r: Tile, n: Neighborhood, analysisArea: Option[GridBounds], target: TargetCell)
  extends FocalCalculation[T](r, n, analysisArea, target)
{
  def traversalStrategy: Option[TraversalStrategy] = None

  def execute(): T = n match {
    case s: Square =>
      val calcSetValue: (Int, Int, Int, Int) => Unit =
        target match {
          case TargetCell.All =>
            { (_, _, x, y) => setValue(x, y) }
          case TargetCell.Data =>
            { (fc, fr, x, y) =>
              if(isData(r.get(fc, fr))) { setValue(x, y) }
              else { copyOriginalValue(fc, fr, x, y) }
            }
          case TargetCell.NoData =>
            { (fc, fr, x, y) =>
              if(isNoData(r.get(fc, fr))) { setValue(x, y) }
              else { copyOriginalValue(fc, fr, x, y) }
            }
        }

      val strategyCalc =
        new CellwiseStrategyCalculation {
          def add(r: Tile, x: Int, y: Int): Unit = CellwiseCalculation.this.add(r, x, y)
          def remove(r: Tile, x: Int, y: Int): Unit = CellwiseCalculation.this.remove(r, x, y)
          def reset(): Unit = CellwiseCalculation.this.reset()
          def setValue(focusCol: Int, focusRow: Int, x: Int, y: Int): Unit =
            calcSetValue(focusCol, focusRow, x, y)
        }

      CellwiseStrategy.execute(r, s, strategyCalc, bounds)
      result
    case _ => sys.error("Cannot use cellwise calculation with this traversal strategy.")
  }

  def add(r: Tile, x: Int, y: Int)
  def remove(r: Tile, x: Int, y: Int)
  def reset(): Unit
  def setValue(x: Int, y: Int)
}

trait CellwiseStrategyCalculation {
  def add(r: Tile, x: Int, y: Int): Unit
  def remove(r: Tile, x: Int, y: Int): Unit
  def reset(): Unit
  def setValue(focusCol: Int, focusRow: Int, x: Int, y: Int): Unit
}

/*
 * Mixin's that define common raster-result functionality
 * for FocalCalculations.
 * Access the resulting raster's array tile through the
 * 'resultTile' member.
 */

/**
  * Defines a focal calculation as returning a [[Tile]] with
  * [[BitArrayTile]], and defines the Initialization.init function for
  * setting up the tile.
  */
trait BitArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[BitArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile = BitArrayTile.empty(rows, cols)

  val copyOriginalValue: (Int, Int, Int, Int) => Unit = { (focusCol: Int, focusRow: Int, col: Int, row: Int) =>
    resultTile.set(col, row, r.get(focusCol, focusRow))
  }

  def result = resultTile
}

/**
  * Defines a focal calculation as returning a [[Tile]] with
  * [[ByteArrayTile]], and defines the Initialization.init function
  * for setting up the tile.
  */
trait ByteArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[ByteArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile = ByteArrayTile.empty(cols, rows)

  val copyOriginalValue: (Int, Int, Int, Int) => Unit = { (focusCol: Int, focusRow: Int, col: Int, row: Int) =>
    resultTile.set(col, row, r.get(focusCol, focusRow))
  }

  def result = resultTile
}

/**
  * Defines a focal calculation as returning a [[Tile]] with
  * [[ShortArrayTile]], and defines the Initialization.init function
  * for setting up the tile.
  */
trait ShortArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[ShortArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile = ShortArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  val copyOriginalValue: (Int, Int, Int, Int) => Unit = { (focusCol: Int, focusRow: Int, col: Int, row: Int) =>
    resultTile.set(col, row, r.get(focusCol, focusRow))
  }

  def result = resultTile
}

/**
  * Defines a focal calculation as returning a [[Tile]] with
  * [[IntArrayTile]], and defines the Initialization.init function for
  * setting up the tile.
  */
trait IntArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[IntArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile = IntArrayTile.empty(cols, rows)

  val copyOriginalValue: (Int, Int, Int, Int) => Unit = { (focusCol: Int, focusRow: Int, col: Int, row: Int) =>
    resultTile.set(col, row, r.get(focusCol, focusRow))
  }

  def result = resultTile
}

/**
  * Defines a focal calculation as returning a [[Tile]] with
  * [[FloatArrayTile]], and defines the Initialization.init function
  * for setting up the tile.
  */
trait FloatArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[FloatArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile = FloatArrayTile.empty(cols, rows)

  val copyOriginalValue: (Int, Int, Int, Int) => Unit = { (focusCol: Int, focusRow: Int, col: Int, row: Int) =>
    resultTile.setDouble(col, row, r.getDouble(focusCol, focusRow))
  }

  def result = resultTile
}

/**
  * Defines a focal calculation as returning a [[Tile]] with
  * [[DoubleArrayTile]], and defines the Initialization.init function
  * for setting up the tile.
  */
trait DoubleArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[DoubleArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile = DoubleArrayTile.empty(cols, rows)

  val copyOriginalValue: (Int, Int, Int, Int) => Unit = { (focusCol: Int, focusRow: Int, col: Int, row: Int) =>
    resultTile.setDouble(col, row, r.getDouble(focusCol, focusRow))
  }

  def result = resultTile
}

trait ArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  def resultCellType: DataType with NoDataHandling = r.cellType
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile: MutableArrayTile = ArrayTile.empty(resultCellType, cols, rows)

  val copyOriginalValue: (Int, Int, Int, Int) => Unit =
    if(!r.cellType.isFloatingPoint) {
      { (focusCol: Int, focusRow: Int, col: Int, row: Int) =>
        resultTile.set(col, row, r.get(focusCol, focusRow))
      }
    } else {
      { (focusCol: Int, focusRow: Int, col: Int, row: Int) =>
        resultTile.setDouble(col, row, r.getDouble(focusCol, focusRow))
      }
    }

  def result = resultTile
}
