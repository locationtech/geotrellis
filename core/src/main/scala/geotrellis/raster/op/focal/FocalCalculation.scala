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

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

/**
 * Declares that implementers have a result
 */
trait Resulting[T] {
  def result: T
}

/**
 * A calculation that a FocalStrategy uses to complete
 * a focal operation.
 */
trait FocalCalculation[T] extends Resulting[T] {
  /**
   * @param re	Optional extent of the analysis area (where the focal operation will be executed)
   */
  def execute(r: Tile, n: Neighborhood, neighbors: Seq[Option[Tile]]): Unit
}

/**
 * A focal calculation that uses the Cursor focal strategy.
 */
trait CursorCalculation[T] extends FocalCalculation[T] {
  def traversalStrategy: Option[TraversalStrategy] = None
  def execute(r: Tile, n: Neighborhood, neighbors: Seq[Option[Tile]]): Unit = 
    CursorStrategy.execute(r, n, this, traversalStrategy, neighbors)
  
  def calc(r: Tile, cur: Cursor): Unit
}

/**
 * A focal calculation that uses the Cellwise focal strategy
 */
trait CellwiseCalculation[T] extends FocalCalculation[T] {
  def traversalStrategy: Option[TraversalStrategy] = None
  def execute(r: Tile, n: Neighborhood, neighbors: Seq[Option[Tile]]) = n match {
      case s: Square => CellwiseStrategy.execute(r, s, this, traversalStrategy, neighbors)
      case _ => sys.error("Cannot use cellwise calculation with this traversal strategy.")
    }
  
  def add(r: Tile, x: Int, y: Int)
  def remove(r: Tile, x: Int, y: Int)
  def reset(): Unit
  def setValue(x: Int, y: Int)
}

/*
 * Trait defining the ability to initialize the focal calculation
 * with a range of variables.
 */

/** Trait defining the ability to initialize the focal calculation with a raster. */
trait Initialization { 
  def init(r: Tile): Unit 
}

/** Trait defining the ability to initialize the focal calculation with a raster and one other parameter. */
trait Initialization1[A]       { def init(r: Tile, a: A): Unit }

/** Trait defining the ability to initialize the focal calculation with a raster and two other parameters. */
trait Initialization2[A, B]     { def init(r: Tile, a: A, b: B): Unit }

/** Trait defining the ability to initialize the focal calculation with a raster and three other parameters. */
trait Initialization3[A, B, C]   { def init(r: Tile, a: A, b: B, c: C): Unit }

/** Trait defining the ability to initialize the focal calculation with a raster and four other parameters. */
trait Initialization4[A, B, C, D] { def init(r: Tile, a: A, b: B, c: C, d: D): Unit }

/*
 * Mixin's that define common raster-result functionality
 * for FocalCalculations.
 * Access the resulting raster's array tile through the 
 * 'tile' member.
 */

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[BitArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait BitArrayTileResult extends Initialization with Resulting[Tile] {
  /** [[BitArrayTile]] that will be returned by the focal calculation */
  var tile: BitArrayTile = null

  var cols: Int = 0
  var rows: Int = 0

  def init(r: Tile) = {
    cols = r.cols
    rows = r.rows
    tile = BitArrayTile.empty(cols, rows)
  }

  def result = tile
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[ByteArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait ByteArrayTileResult extends Initialization with Resulting[Tile] {
  /** [[ByteArrayTile]] that will be returned by the focal calculation */
  var tile: ByteArrayTile = null

  var cols: Int = 0
  var rows: Int = 0

  def init(r: Tile) = {
    cols = r.cols
    rows = r.rows
    tile = ByteArrayTile.empty(cols, rows)
  }

  def result = tile
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[ShortArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait ShortArrayTileResult extends Initialization with Resulting[Tile] {
  /** [[ShortArrayTile]] that will be returned by the focal calculation */
  var tile: ShortArrayTile = null

  var cols: Int = 0
  var rows: Int = 0

  def init(r: Tile) = {
    cols = r.cols
    rows = r.rows
    tile = ShortArrayTile.empty(cols, rows)
  }

  def result = tile
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[IntArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait IntArrayTileResult extends Initialization with Resulting[Tile] {
  /** [[IntArrayTile]] that will be returned by the focal calculation */
  var tile: IntArrayTile = null

  var cols: Int = 0
  var rows: Int = 0

  def init(r: Tile) = {
    cols = r.cols
    rows = r.rows
    tile = IntArrayTile.empty(cols, rows)
  }

  def result = tile
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[FloatArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait FloatArrayTileResult extends Initialization with Resulting[Tile] {
  /** [[FloatArrayTile]] that will be returned by the focal calculation */
  var tile: FloatArrayTile = null

  var cols: Int = 0
  var rows: Int = 0

  def init(r: Tile) = {
    cols = r.cols
    rows = r.rows
    tile = FloatArrayTile.empty(cols, rows)
  }

  def result = tile
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[DoubleArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait DoubleArrayTileResult extends Initialization with Resulting[Tile] {
  /** [[DoubleArrayTile]] that will be returned by the focal calculation */
  var tile: DoubleArrayTile = null

  var cols: Int = 0
  var rows: Int = 0

  def init(r: Tile) = {
    cols = r.cols
    rows = r.rows
    tile = DoubleArrayTile.empty(cols, rows)
  }

  def result = tile
}
