package geotrellis.raster

import geotrellis._

/**
 * This trait represents a raster data which represents a lazily-applied
 * cropping of an underlying raster data object.
 */
case class CroppedTiledRasterData(underlying:TiledRasterData,
                                  underlyingRasterExtent:RasterExtent,
                                  rasterExtent:RasterExtent,
                                  colOffset:Int, rowOffset:Int,
                                  _cols:Int, _rows:Int) extends TiledRasterData {
  override def cols = _cols
  override def rows = _rows

  val tileLayout = initializeTileLayout
  val (leftBorder, topBorder, rightBorder, bottomBorder) = calculateBorder
  val (leftClip, topClip, rightClip, bottomClip) = calculateClipping

  override def mutable:MutableRasterData = {
    val arr = alloc(cols, rows)
    if (isFloat) {
      for (c <- 0 until cols; r <- 0 until rows) {
        arr.setDouble(c, r, getDouble(c, r))
      }
    } else {
      for (c <- 0 until cols; r <- 0 until rows) {
        val start = System.currentTimeMillis
        arr.set(c, r, get(c, r))
        val elapsed = System.currentTimeMillis - start
      }
    }
    arr
  }

  override def force = mutable

  def initializeTileLayout = {
    // get our underlying data's layout
    val TileLayout(_, _, pCols, pRows) = underlying.tileLayout

    // figure out how much "padding" on the left/upper side we need
    val colPad = ((colOffset % pCols) + pCols) % pCols
    val rowPad = ((rowOffset % pRows) + pRows) % pRows

    // based on padding, see how many tiles we need to cover our area
    val tilesCols = (colPad + cols + pCols - 1) / pCols
    val tilesRows = (rowPad + rows + pRows - 1) / pRows

    // build our own layout
    val layout = TileLayout(tilesCols, tilesRows, pCols, pRows)
    layout
  }

  def calculateBorder = {
    // Get our underlying data's layout.
    // baseCols & baseRows describe the cols and rows of underlying tiles.
    // pRows & pCols describe the pixels per tile.
    val TileLayout(baseCols, baseRows, pCols, pRows) = underlying.tileLayout
    // figure out our "upper left tile" border
    // ?? these are negative values?
    val leftBorder = scala.math.floor(colOffset.toDouble / pCols).toInt
    val topBorder = scala.math.floor(rowOffset.toDouble / pRows).toInt

    // figure out our "lower right tile" border
    val rightBorder = tileLayout.tileCols + leftBorder - 1
    val bottomBorder = tileLayout.tileRows + topBorder - 1

    // return a tuple
    (leftBorder, topBorder, rightBorder, bottomBorder)
  }

  def calculateClipping = {
    // get our underlying data's layout
    val TileLayout(_, _, pCols, pRows) = underlying.tileLayout

    // return a tuple
    val left = colOffset % pCols
    val top = rowOffset % pRows
    val right = pCols - ((colOffset + cols) % pCols)
    val bottom = pRows - (rowOffset + rows) % pRows
 
    (colOffset % pCols, rowOffset % pRows, // left and top
     right, pRows - (rowOffset + rows) % pRows) // right and bottom
  }

  def getType = underlying.getType
  def alloc(cols:Int, rows:Int) = underlying.alloc(cols, rows)

  def apply(i:Int) = get(i % cols, i / cols)
  def applyDouble(i:Int) = getDouble(i % cols, i / cols)

  override def get(col:Int, row:Int):Int = {
    val c = col + colOffset
    if (c < 0 || c >= underlying.cols) return NODATA
    val r = row + rowOffset
    if (r < 0 || r >= underlying.rows) return NODATA
    val z = underlying.get(c, r)
    z 
  }

  override def getDouble(col:Int, row:Int):Double = {
    val c = col + colOffset
    if (c < 0 || c >= underlying.cols) return Double.NaN
    val r = row + rowOffset
    if (r < 0 || r >= underlying.rows) return Double.NaN
    underlying.getDouble(c, r)
  }


  override def foreach(f:Int => Unit) = {
    for (c <- 0 until cols; r <- 0 until rows) f(get(c, r))
  }
  
  def getTileBounds(col:Int, row:Int) = {
    val TileLayout(_, _, pCols, pRows) = underlying.tileLayout

    val col1 = if (col == leftBorder) {
      leftClip
    } else {
      0
    }
    val row1 = if (row == topBorder) {
      topClip 
    } else {
      0
    }
    val col2 = if (col == rightBorder) {
      pCols - rightClip 
    } else { 
      tileLayout.pixelCols
    }
    val row2 = if (row == bottomBorder) pRows - bottomClip else tileLayout.pixelRows
    (col1, row1, col2, row2)
  }

  def getTile(col:Int, row:Int):RasterData = {
    val pCols = tileLayout.pixelCols
    val pRows = tileLayout.pixelRows

    if (col < leftBorder || rightBorder < col) return IntConstant(NODATA, pCols, pRows)
    if (row < topBorder || bottomBorder < row) return IntConstant(NODATA, pCols, pRows)
  
    val underlyingCol = col + leftBorder
    val underlyingRow = row + topBorder

    val tile = underlying.getTile(col + leftBorder, row + topBorder)

    val (col1, row1, col2, row2) = getTileBounds(underlyingCol, underlyingRow)

    // if we need to do some clipping, use a lazy raster data to limit
    // our data to the relevant box. otherwise, return the whole tile.
    if (col1 != 0 || row1 != 0 || col2 != pCols || row2 != pRows) {
      // TODO: fix me please! getTile should return ArrayRasterData
      MaskedArrayRasterData(tile.asInstanceOf[ArrayRasterData], col1, row1, col2, row2)
    } else {
      tile
    }
  }

  def getTileOp(rl:ResolutionLayout, c:Int, r:Int) = {
  
    val url = underlying.tileLayout.getResolutionLayout(underlyingRasterExtent)
 
    val underlyingTileCol = c + leftBorder
    val underlyingTileRow = r + topBorder 

    val tileOp = underlying.getTileOp(url, c + leftBorder, r + topBorder)
  
    val pCols = tileLayout.pixelCols
    val pRows = tileLayout.pixelRows
  
    val (col1, row1, col2, row2) = getTileBounds(underlyingTileCol, underlyingTileRow)
  
    if (col1 != 0 || row1 != 0 || col2 != pCols || row2 != pRows) {
      logic.Do(tileOp) {
        r =>
        val rr = r.data.asInstanceOf[ArrayRasterData]
        Raster(MaskedArrayRasterData(rr, col1, row1, col2, row2), r.rasterExtent)
      }
    } else {
      tileOp
    }
  }
}
