/***
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
 ***/

package geotrellis.raster

import geotrellis._

object TileLayout {
  def apply(re:RasterExtent, tileCols:Int, tileRows:Int):TileLayout =
    TileLayout(tileCols,tileRows,math.ceil(re.cols/tileCols).toInt,math.ceil(re.rows/tileRows).toInt)

  def fromTileDimensions(re:RasterExtent, pixelCols:Int, pixelRows:Int):TileLayout = {
    val tileCols = (re.cols + pixelCols - 1) / pixelCols
    val tileRows = (re.rows + pixelRows - 1) / pixelRows
    TileLayout(tileCols, tileRows, pixelCols, pixelRows)
  }

  def singleTile(cols:Int,rows:Int) =
    TileLayout(1,1,cols,rows)

  def singleTile(re:RasterExtent) =
    TileLayout(1,1,re.cols,re.rows)
}

/**
 * This class stores the layout of a tiled raster: the number of tiles (in
 * cols/rows) and also the size of each tile (in cols/rows of pixels).
 */
case class TileLayout(tileCols:Int, tileRows:Int, pixelCols:Int, pixelRows:Int) {
  def isTiled = tileCols > 1 || tileRows > 1

  /**
   * Return the total number of columns across all the tiles.
   */
  def totalCols = tileCols * pixelCols

  /**
   * Return the total number of rows across all the tiles.
   */
  def totalRows = tileRows * pixelRows

  /**
   * Given a particular RasterExtent (geographic area plus resolution) for the
   * entire tiled raster, construct an ResolutionLayout which will manage the
   * appropriate geographic boundaries, and resolution information, for each
   * tile.
   */
  def getResolutionLayout(re:RasterExtent) = {
    ResolutionLayout(re, pixelCols, pixelRows)
  }

  /** Gets the index of a tile at col, row. */
  def getTileIndex(col:Int,row:Int) =
    row*tileCols + col
    
  def getXY(tileIndex: Int): Tuple2[Int, Int] = {
    val ty = tileIndex / tileCols
    val tx = tileIndex - (ty * tileCols)
    (tx, ty)
  }
}

/**
 * For a particular resolution and tile layout, this class stores the
 * geographical boundaries of each tile extent.
 */
case class ResolutionLayout(re:RasterExtent, pixelCols:Int, pixelRows:Int) {

  /**
   * Given an extent and resolution (RasterExtent), return the geographic
   * X-coordinates for each tile boundary in this raster data. For example,
   * if we have a 2x2 RasterData, with a raster extent whose X coordinates
   * span 13.0 - 83.0 (i.e. cellwidth is 35.0), we would return the following
   * for the corresponding input:
   *
   *  Input     Output
   * -------   -------- 
   *    0        13.0
   *    1        48.0
   *    2        83.0
   *
   */
  def getXCoord(col:Int):Double =
    re.extent.xmin + (col * re.cellwidth * pixelCols)

  /**
   * This method is identical to getXCoord except that it functions on the
   * Y-axis instead.
   * 
   * Note that the origin tile (0,0) is in the upper left of the extent, so the
   * upper left corner of the origin tile is (xmin, ymax).
   */
  def getYCoord(row:Int):Double = 
    re.extent.ymax - (row * re.cellheight * pixelRows)

  def getExtent(c:Int, r:Int) = {
    Extent(getXCoord(c), getYCoord(r + 1), getXCoord(c + 1), getYCoord(r))
  }

  def getRasterExtent(c:Int, r:Int) = 
    RasterExtent(getExtent(c, r), re.cellwidth, re.cellheight, pixelCols, pixelRows)
}
