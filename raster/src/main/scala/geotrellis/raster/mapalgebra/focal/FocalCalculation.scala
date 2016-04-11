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

/**
 * Declares that implementers have a result
 */
trait Resulting[T] {
  def result: T
}

sealed trait TargetCell extends Serializable {
  def mask(tile: MutableArrayTile): MutableArrayTile
}
object TargetCell {
  object NoData extends TargetCell {
    override def mask(tile: MutableArrayTile): MutableArrayTile =
      new MaskedMutableArrayTile(true, tile)
  }
  object Data extends TargetCell {
    override def mask(tile: MutableArrayTile): MutableArrayTile =
      new MaskedMutableArrayTile(false, tile)
  }
  object All extends TargetCell {
    override def mask(tile: MutableArrayTile): MutableArrayTile = tile
  }
}

private class MaskedMutableArrayTile(changeData: Boolean, val tile: MutableArrayTile) extends MutableArrayTile {
  def update(i: Int, z: Int): Unit =
    if (changeData ^ isData(tile.apply(i))) tile.update(i, z)
  def updateDouble(i: Int, z: Double): Unit =
    if (changeData ^ isData(tile.applyDouble(i))) tile.updateDouble(i, z)
  def apply(i: Int): Int = tile.apply(i)
  def applyDouble(i: Int): Double = tile.applyDouble(i)
  def copy = new MaskedMutableArrayTile(changeData, tile.copy.mutable)
  def toBytes = tile.toBytes
  def cellType = tile.cellType
  def cols = tile.cols
  def rows = tile.rows
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
    CursorStrategy.execute(cursor, { () => calc(tile, cursor) }, bounds, traversalStrategy)
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
    CursorStrategy.execute(cursor, { () => calc(tile, cursor) }, bounds, traversalStrategy)
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
      CellwiseStrategy.execute(r, s, this, bounds)
      result
    case _ => sys.error("Cannot use cellwise calculation with this traversal strategy.")
  }

  def add(r: Tile, x: Int, y: Int)
  def remove(r: Tile, x: Int, y: Int)
  def reset(): Unit
  def setValue(x: Int, y: Int)
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
  val resultTile = target.mask(BitArrayTile.empty(rows, cols))

  def result =
    resultTile match {
      case (t: MaskedMutableArrayTile) => t.tile
      case _ => resultTile
    }
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
  val resultTile = target.mask(ByteArrayTile.empty(cols, rows))

  def result =
    resultTile match {
      case (t: MaskedMutableArrayTile) => t.tile
      case _ => resultTile
    }
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
  val resultTile = target.mask(ShortArrayTile(Array.ofDim[Short](cols * rows), cols, rows))

  def result =
    resultTile match {
      case (t: MaskedMutableArrayTile) => t.tile
      case _ => resultTile
    }
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
  val resultTile = target.mask(IntArrayTile.empty(cols, rows))

  def result =
    resultTile match {
      case (t: MaskedMutableArrayTile) => t.tile
      case _ => resultTile
    }
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
  val resultTile = target.mask(FloatArrayTile.empty(cols, rows))

  def result =
    resultTile match {
      case (t: MaskedMutableArrayTile) => t.tile
      case _ => resultTile
    }
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
  val resultTile = target.mask(DoubleArrayTile.empty(cols, rows))

  def result =
    resultTile match {
      case (t: MaskedMutableArrayTile) => t.tile
      case _ => resultTile
    }
}

trait ArrayTileResult extends Resulting[Tile] { self: FocalCalculation[Tile] =>
  def resultCellType: DataType with NoDataHandling = r.cellType
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val resultTile: MutableArrayTile = target.mask(ArrayTile.empty(resultCellType, cols, rows))

  def result =
    resultTile match {
      case (t: MaskedMutableArrayTile) => t.tile
      case _ => resultTile
    }
}
