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

import geotrellis.raster.split.Split
import geotrellis.vector.Extent

import spire.syntax.cfor._

import scala.collection.mutable

/**
  * The companion object for the [[CompositeTile]] type.
  */
object CompositeTile {

  /**
    * Given a [[Tile]] and a [[TileLayout]], produce a
    * [[CompositeTile]].
    *
    * @param   tile        The tile
    * @param   tileLayout  The layout
    *
    * @return              The CompositeTile
    */
  def apply(tile: Tile, tileLayout: TileLayout): CompositeTile =
    tile match {
      case ct: CompositeTile =>
        if(tileLayout != ct.tileLayout) {
          throw new GeoAttrsError("This tile is a composite tile with a different layout than" +
                                  " the argument tile layout." +
                                 s" $tileLayout does not match ${ct.tileLayout}")
        }
        ct
      case _ =>
        wrap(tile, tileLayout)
    }

  /**
    * Converts a [[Tile]] into a [[CompositeTile]] with the given tile
    * layout.
    *
    * @param        tile           Tile to wrap.
    * @param        tileCols       Number of tile columns of the resulting CompositeTile.
    * @param        tileRows       Number of tile columns of the resulting CompositeTile.
    *
    * @return                      The CompositeTile
    */
  def wrap(tile: Tile, tileCols: Int, tileRows: Int): CompositeTile = {
    val tileLayout = TileLayout(tileCols, tileRows, ((tile.cols - 1) / tileCols) + 1, ((tile.rows - 1) / tileRows) + 1)
    wrap(tile, tileLayout, true)
  }

  /**
    * Converts a [[Tile]] into a [[CompositeTile]] with the given tile
    * layout.  Set the 'cropped' flag to false if you want the tiles
    * to be [[ArrayTile]]s, otherwise they will be [[CroppedTile]]s
    * with the Tile 'tile' as the backing.
    *
    * @param        tile           Tile to wrap.
    * @param        tileCols       Number of tile columns of the resulting CompositeTile.
    * @param        tileRows       Number of tile columns of the resulting CompositeTile.
    * @param        cropped        Boolean to control cropping
    *
    * @return                      The CompositeTile
    */
  def wrap(tile: Tile, tileCols: Int, tileRows: Int, cropped: Boolean): CompositeTile = {
    val tileLayout = TileLayout(tileCols, tileRows, ((tile.cols - 1) / tileCols) + 1, ((tile.rows - 1) / tileRows) + 1)
    wrap(tile, tileLayout, cropped)
  }

  /**
    * Converts a [[Tile]] into a [[CompositeTile]] with the given
    * [[TileLayout]].
    *
    * @param        tile           Tile to wrap.
    * @param        tileLayout     TileLayout of the resulting CompositeTile.
    *
    * @return                      The CompositeTile
    */
  def wrap(tile: Tile, tileLayout: TileLayout): CompositeTile =
    CompositeTile(tile.split(tileLayout, Split.Options(cropped = true)), tileLayout)

  /**
    * Converts a [[Tile]] into a [[CompositeTile]] with the given
    * [[TileLayout]].  Set the 'cropped' flag to false if you want the
    * tiles to be [[ArrayTile]]s, otherwise they will be
    * [[CroppedTile]]s with the Tile 'tile' as the backing.
    *
    * @param        tile           Tile to wrap.
    * @param        tileLayout     TileLayout of the resulting CompositeTile.
    * @param        cropped        Boolean to control cropping
    *
    * @return                      The CompositeTile
    */
  def wrap(tile: Tile, tileLayout: TileLayout, cropped: Boolean): CompositeTile =
    CompositeTile(tile.split(tileLayout, Split.Options(cropped = cropped)), tileLayout)

}

/**
  * The [[CompositeTile]] type.
  */
case class CompositeTile(tiles: Seq[Tile],
                         tileLayout: TileLayout) extends Tile {
  assert(tileLayout.totalCols.isValidInt, "Total cols is not integer, cannot create such a large composite tile.")
  assert(tileLayout.totalRows.isValidInt, "Total rows is not integer, cannot create such a large composite tile.")
  val cols = tileLayout.totalCols.toInt
  val rows = tileLayout.totalRows.toInt

  private val tileList = tiles.toList
  private val tileCols = tileLayout.layoutCols
  private def getTile(tcol: Int, trow: Int) = tileList(trow * tileCols + tcol)

  val cellType: CellType = tiles(0).cellType

  /**
    * Returns a [[Tile]] equivalent to this [[CompositeTile]], except
    * with cells of the given type.
    *
    * @param   targetCellType  The type of cells that the result should have
    * @return                  The new Tile
    */
  def convert(targetCellType: CellType): Tile =
    mutable(targetCellType)

  def withNoData(noDataValue: Option[Double]): CompositeTile =
    CompositeTile(tiles.map(_.withNoData(noDataValue)), tileLayout)

  def interpretAs(targetCellType: CellType): CompositeTile =
    CompositeTile(tiles.map(_.interpretAs(targetCellType)), tileLayout)

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this [[CompositeTile]].
    *
    * @return  The MutableArrayTile
    */
  def mutable(): MutableArrayTile =
    mutable(cellType)

  /**
    * Return the [[MutableArrayTile]] equivalent of this [[CompositeTile]].
    *
    * @return  The MutableArrayTile
    */
  def mutable(targetCellType: CellType): MutableArrayTile = {
    if (cols.toLong * rows.toLong > Int.MaxValue.toLong) {
      sys.error("This tiled raster is too big to convert into an array.")
    } else {
      if(targetCellType.isFloatingPoint != cellType.isFloatingPoint)
        logger.warn(s"Conversion from $cellType to $targetCellType may lead to data loss.")

      val tile = ArrayTile.alloc(targetCellType, cols, rows)
      val len = cols * rows
      val layoutCols = tileLayout.layoutCols
      val layoutRows = tileLayout.layoutRows
      val tileCols = tileLayout.tileCols
      val tileRows = tileLayout.tileRows
      if(!cellType.isFloatingPoint) {
        cfor(0)(_ < layoutRows, _ + 1) { trow =>
          cfor(0)(_ < layoutCols, _ + 1) { tcol =>
            val sourceTile = getTile(tcol, trow)
            cfor(0)(_ < tileRows, _ + 1) { prow =>
              cfor(0)(_ < tileCols, _ + 1) { pcol =>
                val acol = (tileCols * tcol) + pcol
                val arow = (tileRows * trow) + prow
                tile.set(acol, arow, sourceTile.get(pcol, prow))
              }
            }
          }
        }
      } else {
        cfor(0)(_ < layoutRows, _ + 1) { trow =>
          cfor(0)(_ < layoutCols, _ + 1) { tcol =>
            val sourceTile = getTile(tcol, trow)
            cfor(0)(_ < tileRows, _ + 1) { prow =>
              cfor(0)(_ < tileCols, _ + 1) { pcol =>
                val acol = (tileCols * tcol) + pcol
                val arow = (tileRows * trow) + prow
                tile.setDouble(acol, arow, sourceTile.getDouble(pcol, prow))
              }
            }
          }
        }
      }
      tile
    }
  }

  /**
    * Return a copy of the data contained in this tile as an array.
    *
    * @return  The array of integers
    */
  def toArray(): Array[Int] = {
    if (cols.toLong * rows.toLong > Int.MaxValue.toLong) {
      sys.error("This tiled raster is too big to convert into an array.")
    } else {
      val arr = Array.ofDim[Int](cols * rows)
      val len = cols * rows
      val layoutCols = tileLayout.layoutCols
      val layoutRows = tileLayout.layoutRows
      val tileCols = tileLayout.tileCols
      val tileRows = tileLayout.tileRows
      val totalCols = layoutCols * tileCols

      cfor(0)(_ < layoutRows, _ + 1) { trow =>
        cfor(0)(_ < layoutCols, _ + 1) { tcol =>
          val tile = getTile(tcol, trow)
          cfor(0)(_ < tileRows, _ + 1) { prow =>
            cfor(0)(_ < tileCols, _ + 1) { pcol =>
              val acol = (tileCols * tcol) + pcol
              val arow = (tileRows * trow) + prow
              arr(arow * totalCols + acol) = tile.get(pcol, prow)
            }
          }
        }
      }
      arr
    }
  }

  /**
    * Return a copy of the data contained in this tile as an array of
    * doubles.
    *
    * @return  The array of doubles
    */
  def toArrayDouble(): Array[Double] = {
    if (cols.toLong * rows.toLong > Int.MaxValue.toLong) {
      sys.error("This tiled raster is too big to convert into an array.")
    } else {
      val arr = Array.ofDim[Double](cols * rows)
      val len = cols * rows
      val layoutCols = tileLayout.layoutCols
      val layoutRows = tileLayout.layoutRows
      val tileCols = tileLayout.tileCols
      val tileRows = tileLayout.tileRows
      val totalCols = layoutCols * tileCols

      cfor(0)(_ < layoutRows, _ + 1) { trow =>
        cfor(0)(_ < layoutCols, _ + 1) { tcol =>
          val tile = getTile(tcol, trow)
          cfor(0)(_ < tileRows, _ + 1) { prow =>
            cfor(0)(_ < tileCols, _ + 1) { pcol =>
              val acol = (tileCols * tcol) + pcol
              val arow = (tileRows * trow) + prow
              arr(arow * totalCols + acol) = tile.getDouble(pcol, prow)
            }
          }
        }
      }
      arr
    }
  }

  /**
    * Return the underlying data behind this [[CompositeTile]] as an
    * array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = toArrayTile.toBytes

  /**
    * Fetch the datum at the given column and row of the
    * [[CompositeTile]].
    *
    * @param   col  The column
    * @param   row  The row
    * @return       The Int datum found at the given location
    */
  def get(col: Int, row: Int): Int = {
    val tcol = col / tileLayout.tileCols
    val trow = row / tileLayout.tileRows
    val pcol = col % tileLayout.tileCols
    val prow = row % tileLayout.tileRows

    getTile(tcol, trow).get(pcol, prow)
  }

  /**
    * Fetch the datum at the given column and row of the
    * [[CompositeTile]].
    *
    * @param   col  The column
    * @param   row  The row
    * @return       The Double datum found at the given location
    */
  def getDouble(col: Int, row: Int): Double = {
    val tcol = col / tileLayout.tileCols
    val trow = row / tileLayout.tileRows
    val pcol = col % tileLayout.tileCols
    val prow = row % tileLayout.tileRows
    getTile(tcol, trow).getDouble(pcol, prow)
  }

  /**
    * Execute a function on each cell of the [[CompositeTile]].
    *
    * @param  f  A function from Int to Unit.  Presumably, the function is executed for side-effects.
    */
  def foreach(f: Int => Unit): Unit = {
    val layoutCols = tileLayout.layoutCols
    val layoutRows = tileLayout.layoutRows
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    cfor(0)(_ < layoutRows, _ + 1) { trow =>
      cfor(0)(_ < layoutCols, _ + 1) { tcol =>
        val tile = getTile(tcol, trow)
        cfor(0)(_ < tileRows, _ + 1) { prow =>
          cfor(0)(_ < tileCols, _ + 1) { pcol =>
            f(tile.get(pcol, prow))
          }
        }
      }
    }
  }

  /**
    * Execute a function on each cell of the [[CompositeTile]].
    *
    * @param  f  A function from Double to Unit.  Presumably, the function is executed for side-effects.
    */
  def foreachDouble(f: Double => Unit): Unit = {
    val layoutCols = tileLayout.layoutCols
    val layoutRows = tileLayout.layoutRows
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    cfor(0)(_ < layoutRows, _ + 1) { trow =>
      cfor(0)(_ < layoutCols, _ + 1) { tcol =>
        val tile = getTile(tcol, trow)
        cfor(0)(_ < tileRows, _ + 1) { prow =>
          cfor(0)(_ < tileCols, _ + 1) { pcol =>
            f(tile.getDouble(pcol, prow))
          }
        }
      }
    }
  }

  /**
    * Execute an [[IntTileVisitor]] at each cell of the
    * [[CompositeTile]].
    *
    * @param  visitor  An IntTileVisitor
    */
  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    val layoutCols = tileLayout.layoutCols
    val layoutRows = tileLayout.layoutRows
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    cfor(0)(_ < layoutRows, _ + 1) { trow =>
      cfor(0)(_ < layoutCols, _ + 1) { tcol =>
        val tile = getTile(tcol, trow)
        cfor(0)(_ < tileRows, _ + 1) { prow =>
          cfor(0)(_ < tileCols, _ + 1) { pcol =>
            val acol = (tileCols * tcol) + pcol
            val arow = (tileRows * trow) + prow
            visitor(acol, arow, tile.get(pcol, prow))
          }
        }
      }
    }
  }

  /**
    * Execute an [[DoubleTileVisitor]] at each cell of the
    * [[CompositeTile]].
    *
    * @param  visitor  A DoubleTileVisitor
    */
  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = {
    val layoutCols = tileLayout.layoutCols
    val layoutRows = tileLayout.layoutRows
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    cfor(0)(_ < layoutRows, _ + 1) { trow =>
      cfor(0)(_ < layoutCols, _ + 1) { tcol =>
        val tile = getTile(tcol, trow)
        cfor(0)(_ < tileRows, _ + 1) { prow =>
          cfor(0)(_ < tileCols, _ + 1) { pcol =>
            val acol = (tileCols * tcol) + pcol
            val arow = (tileRows * trow) + prow
            visitor(acol, arow, tile.getDouble(pcol, prow))
          }
        }
      }
    }
  }

  /**
    * Map each cell in the given raster to a new one, using the given
    * function.
    *
    * @param   f  A function from Int to Int, executed at each point of the tile
    * @return     The result, a [[Tile]]
    */
  def map(f: Int => Int): Tile = {
    val result = ArrayTile.alloc(cellType, cols, rows)
    val layoutCols = tileLayout.layoutCols
    val layoutRows = tileLayout.layoutRows
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    cfor(0)(_ < layoutRows, _ + 1) { trow =>
      cfor(0)(_ < layoutCols, _ + 1) { tcol =>
        val tile = getTile(tcol, trow)
        cfor(0)(_ < tileRows, _ + 1) { prow =>
          cfor(0)(_ < tileCols, _ + 1) { pcol =>
            val acol = (tileCols * tcol) + pcol
            val arow = (tileRows * trow) + prow
            result.set(acol, arow, f(tile.get(pcol, prow)))
          }
        }
      }
    }

    result
  }

  /**
    * Map each cell in the given raster to a new one, using the given
    * function.
    *
    * @param   f  A function from Double to Double, executed at each point of the tile
    * @return     The result, a [[Tile]]
    */
  def mapDouble(f: Double =>Double): Tile = {
    val result = ArrayTile.alloc(cellType, cols, rows)
    val layoutCols = tileLayout.layoutCols
    val layoutRows = tileLayout.layoutRows
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    cfor(0)(_ < layoutRows, _ + 1) { trow =>
      cfor(0)(_ < layoutCols, _ + 1) { tcol =>
        val tile = getTile(tcol, trow)
        cfor(0)(_ < tileRows, _ + 1) { prow =>
          cfor(0)(_ < tileCols, _ + 1) { pcol =>
            val acol = (tileCols * tcol) + pcol
            val arow = (tileRows * trow) + prow
            result.setDouble(acol, arow, f(tile.getDouble(pcol, prow)))
          }
        }
      }
    }

    result
  }

  /**
    * Map an [[IntTileMapper]] over the present tile.
    *
    * @param   mapper  The mapper
    * @return          The result, a [[Tile]]
    */
  def mapIntMapper(mapper: IntTileMapper): Tile = {
    val result = ArrayTile.alloc(cellType, cols, rows)
    val layoutCols = tileLayout.layoutCols
    val layoutRows = tileLayout.layoutRows
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    cfor(0)(_ < layoutRows, _ + 1) { trow =>
      cfor(0)(_ < layoutCols, _ + 1) { tcol =>
        val tile = getTile(tcol, trow)
        cfor(0)(_ < tileRows, _ + 1) { prow =>
          cfor(0)(_ < tileCols, _ + 1) { pcol =>
            val acol = (tileCols * tcol) + pcol
            val arow = (tileRows * trow) + prow
            result.set(acol, arow, mapper(acol, arow, tile.get(pcol, prow)))
          }
        }
      }
    }

    result
  }

  /**
    * Map an [[DoubleTileMapper]] over the present tile.
    *
    * @param   mapper  The mapper
    * @return          The result, a [[Tile]]
    */
  def mapDoubleMapper(mapper: DoubleTileMapper): Tile = {
    val result = ArrayTile.alloc(cellType, cols, rows)
    val layoutCols = tileLayout.layoutCols
    val layoutRows = tileLayout.layoutRows
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    cfor(0)(_ < layoutRows, _ + 1) { trow =>
      cfor(0)(_ < layoutCols, _ + 1) { tcol =>
        val tile = getTile(tcol, trow)
        cfor(0)(_ < tileRows, _ + 1) { prow =>
          cfor(0)(_ < tileCols, _ + 1) { pcol =>
            val acol = (tileCols * tcol) + pcol
            val arow = (tileRows * trow) + prow
            result.setDouble(acol, arow, mapper(acol, arow, tile.getDouble(pcol, prow)))
          }
        }
      }
    }

    result
  }

  /**
    * Combine two [[CompositeTile]]s' cells into new cells using the
    * given integer function. For every (x, y) cell coordinate, get
    * each of the Tiles' integer value, map them to a new value, and
    * assign it to the output's (x, y) cell.
    *
    * @param   other  The other [[Tile]]
    * @param   f      A function from (Int, Int) to Int, the respective arguments are from the respective Tiles
    * @return         The result, an Tile
    */
  def combine(other: Tile)(f: (Int, Int) => Int): Tile = {
    (this, other).assertEqualDimensions

    val result = ArrayTile.alloc(cellType, cols, rows)
    val layoutCols = tileLayout.layoutCols
    val layoutRows = tileLayout.layoutRows
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    cfor(0)(_ < layoutRows, _ + 1) { trow =>
      cfor(0)(_ < layoutCols, _ + 1) { tcol =>
        val tile = getTile(tcol, trow)
        cfor(0)(_ < tileRows, _ + 1) { prow =>
          cfor(0)(_ < tileCols, _ + 1) { pcol =>
            val acol = (tileCols * tcol) + pcol
            val arow = (tileRows * trow) + prow
            result.set(acol, arow, f(tile.get(pcol, prow), other.get(acol, arow)))
          }
        }
      }
    }

    result
  }

  /**
    * Combine two [[CompositeTile]]s' cells into new cells using the
    * given double function. For every (x, y) cell coordinate, get
    * each of the Tiles' integer value, map them to a new value, and
    * assign it to the output's (x, y) cell.
    *
    * @param   other  The other [[Tile]]
    * @param   f      A function from (Int, Int) to Int, the respective arguments are from the respective Tiles
    * @return         The result, an Tile
    */
  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = {
    (this, other).assertEqualDimensions

    val result = ArrayTile.alloc(cellType, cols, rows)
    val layoutCols = tileLayout.layoutCols
    val layoutRows = tileLayout.layoutRows
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    cfor(0)(_ < layoutRows, _ + 1) { trow =>
      cfor(0)(_ < layoutCols, _ + 1) { tcol =>
        val tile = getTile(tcol, trow)
        cfor(0)(_ < tileRows, _ + 1) { prow =>
          cfor(0)(_ < tileCols, _ + 1) { pcol =>
            val acol = (tileCols * tcol) + pcol
            val arow = (tileRows * trow) + prow
            result.setDouble(acol, arow, f(tile.getDouble(pcol, prow), other.getDouble(acol, arow)))
          }
        }
      }
    }

    result
  }

  /**
    * Return an ascii-art representation of this Tile.
    *
    * @return  A string containing the ascii art
    */
  def asciiDraw(): String = {
    val sb = new StringBuilder
    for(layoutRow <- 0 until tileLayout.layoutRows) {
      for(row <- 0 until tileLayout.tileRows) {
        for(layoutCol <- 0 until tileLayout.layoutCols) {
          val tile = getTile(layoutCol, layoutRow)

          for(col <- 0 until tileLayout.tileCols) {
            val v = tile.get(col, row)
            val s = if(isNoData(v)) {
              "ND"
            } else {
              s"$v"
            }
            val pad = " " * math.max(6 - s.size, 0)
            sb.append(s"$pad$s")
          }
          if(layoutCol != tileLayout.layoutCols - 1) {
            val pad = " " * 5
            sb.append(s"$pad| ")r
          }
        }
        sb.append(s"\n")
      }
      if(layoutRow != tileLayout.layoutRows - 1) {
        val rowDiv = "-" * (6 * tileLayout.tileCols * tileLayout.layoutCols - 2) +
                     "-" * (6 * tileLayout.layoutCols)
        sb.append(s"  $rowDiv\n")
      }
    }

    sb.toString
  }
}
