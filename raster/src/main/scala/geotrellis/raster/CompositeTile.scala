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

package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._
import scala.collection.mutable

object CompositeTile {
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

  /** Converts a raster into a CompositeTile with the given tileLayout.
    * 
    * @param        tile              Tile to wrap.
    * @param        tileCols       Number of tile columns of the resulting 
    *                              CompositeTile.
    * @param        tileRows       Number of tile columns of the resulting 
    *                              CompositeTile.
    */ 
  def wrap(tile: Tile, tileCols: Int, tileRows: Int): CompositeTile = {
    val tileLayout = TileLayout(tileCols, tileRows, ((tile.cols - 1) / tileCols) + 1, ((tile.rows - 1) / tileRows) + 1)
    wrap(tile, tileLayout, true)
  }

  /** Converts a raster into a CompositeTile with the given tileLayout.
    * 
    * @param        tile           Tile to wrap.
    * @param        tileCols       Number of tile columns of the resulting 
    *                              CompositeTile.
    * @param        tileRows       Number of tile columns of the resulting 
    *                              CompositeTile.
    * @param        cropped        Set this flag to false if you
    *                              want the tiles to be ArrayTiles,
    *                              otherwise they will be CroppedTiles
    *                              with the raster 'r' as the backing raster.
    */ 
  def wrap(tile: Tile, tileCols: Int, tileRows: Int, cropped: Boolean): CompositeTile = {
    val tileLayout = TileLayout(tileCols, tileRows, ((tile.cols - 1) / tileCols) + 1, ((tile.rows - 1) / tileRows) + 1)
    wrap(tile, tileLayout, cropped)
  }

  /** Converts a raster into a CompositeTile with the given tileLayout.
    * 
    * @param        tile           Tile to wrap.
    * @param        tileLayout     TileLayout of the resulting 
    *                              CompositeTile.
    */ 
  def wrap(tile: Tile, tileLayout: TileLayout): CompositeTile =
    CompositeTile(split(tile, tileLayout, true), tileLayout)

  /** Converts a raster into a CompositeTile with the given tileLayout.
    * 
    * @param        tile           Tile to wrap.
    * @param        tileLayout     TileLayout of the resulting 
    *                              CompositeTile.
    * @param        cropped        Set this flag to false if you
    *                              want the tiles to be ArrayTiles,
    *                              otherwise they will be CroppedTiles
    *                              with the raster 'r' as the backing raster.
    */ 
  def wrap(tile: Tile, tileLayout: TileLayout, cropped: Boolean): CompositeTile =
    CompositeTile(split(tile, tileLayout, cropped), tileLayout)

  /** Splits a raster into a CompositeTile into tiles.
    * 
    * @param        tile           Tile to split.
    * 
    * @param        tileLayout     TileLayout defining the tiles to be 
    *                              generated.
    * 
    * @param        cropped        Set this flag to false if you
    *                              want the tiles to be ArrayTiles,
    *                              otherwise they will be CroppedTiles
    *                              with the raster 'r' as the backing raster.
    * 
    * @param        extend         Set this flag to false if you do not want the
    *                              resulting tiles to extend past the input Tile's
    *                              cols and rows based on the input tileLayout. For
    *                              instance, if the tile layout has tileRows = 50,
    *                              the input raster has rows = 90, and extend is false,
    *                              the tiles of the last row will have rows = 40 instead of rows = 50.
    */ 
  def split(tile: Tile, tileLayout: TileLayout, cropped: Boolean = true, extend: Boolean = true): Array[Tile] = {
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    val tiles = Array.ofDim[Tile](tileLayout.layoutCols * tileLayout.layoutRows)
    cfor(0)(_ < tileLayout.layoutRows, _ + 1) { layoutRow =>
      cfor(0)(_ < tileLayout.layoutCols, _ + 1) { layoutCol =>
        val firstCol = layoutCol * tileCols
        val lastCol = {
          val x = firstCol + tileCols - 1
          if(!extend && x > tile.cols - 1) tile.cols - 1
          else x
        }
        val firstRow = layoutRow * tileRows
        val lastRow = {
          val x = firstRow + tileRows - 1
          if(!extend && x > tile.rows - 1) tile.rows - 1
          else x
        }
        val gb = GridBounds(firstCol, firstRow, lastCol, lastRow)
        tiles(layoutRow * tileLayout.layoutCols + layoutCol) =
          if(cropped) CroppedTile(tile, gb)
          else CroppedTile(tile, gb).toArrayTile
      }
    }
    return tiles
  }
}

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

  def convert(cellType: CellType): Tile =
    LazyConvertedArrayTile(this, cellType)

  def toArrayTile(): ArrayTile = mutable

  def mutable(): MutableArrayTile = {
    if (cols.toLong * rows.toLong > Int.MaxValue.toLong) {
      sys.error("This tiled raster is too big to convert into an array.") 
    } else {
      val tile = ArrayTile.alloc(cellType, cols, rows)
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

  def toBytes(): Array[Byte] = toArrayTile.toBytes

  def get(col: Int, row: Int): Int = {
    val tcol = col / tileLayout.tileCols
    val trow = row / tileLayout.tileRows
    val pcol = col % tileLayout.tileCols
    val prow = row % tileLayout.tileRows

    getTile(tcol, trow).get(pcol, prow)
  }

  def getDouble(col: Int, row: Int): Double = {
    val tcol = col / tileLayout.tileCols
    val trow = row / tileLayout.tileRows
    val pcol = col % tileLayout.tileCols
    val prow = row % tileLayout.tileRows
    getTile(tcol, trow).getDouble(pcol, prow)
  }

  def foreach(f: Int => Unit): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        f(get(col, row))
      }
    }
  }

  def foreachDouble(f: Double => Unit): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        f(getDouble(col, row))
      }
    }
  }

  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        visitor(col, row, get(col, row))
      }
    }
  }

  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        visitor(col, row, getDouble(col, row))
      }
    }
  }

  def map(f: Int => Int): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.set(col, row, f(get(col, row)))
      }
    }
    tile
  }

  def mapDouble(f: Double =>Double): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.setDouble(col, row, f(getDouble(col, row)))
      }
    }
    tile
  }

  def mapIntMapper(mapper: IntTileMapper): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.set(col, row, mapper(col, row, get(col, row)))
      }
    }
    tile
  }

  def mapDoubleMapper(mapper: DoubleTileMapper): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.setDouble(col, row, mapper(col, row, getDouble(col, row)))
      }
    }
    tile
  }

  def combine(other: Tile)(f: (Int, Int) => Int): Tile = {
    (this, other).assertEqualDimensions

    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.set(col, row, f(get(col, row), other.get(col, row)))
      }
    }
    tile
  }

  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = {
    (this, other).assertEqualDimensions

    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.setDouble(col, row, f(getDouble(col, row), other.getDouble(col, row)))
      }
    }
    tile
  }

  override
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
