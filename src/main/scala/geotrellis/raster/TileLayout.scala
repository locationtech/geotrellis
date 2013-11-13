package geotrellis.raster

import geotrellis._
import geotrellis.util.Filesystem
import geotrellis.process._
import geotrellis.data.arg.{ArgWriter,ArgReader}
import geotrellis.feature.Polygon
import java.io.{FileOutputStream, BufferedOutputStream}
import geotrellis.util.Filesystem

object TileLayout {
  def apply(re:RasterExtent, tileCols:Int, tileRows:Int):TileLayout =
    TileLayout(tileCols,tileRows,math.ceil(re.cols/tileCols).toInt,math.ceil(re.rows/tileRows).toInt)
}

/**
 * This class stores the layout of a tiled raster: the number of tiles (in
 * cols/rows) and also the size of each tile (in cols/rows of pixels).
 */
case class TileLayout(tileCols:Int, tileRows:Int, pixelCols:Int, pixelRows:Int) {

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
