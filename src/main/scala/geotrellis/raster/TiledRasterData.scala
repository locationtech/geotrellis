package geotrellis.raster

import geotrellis._
import geotrellis.data.{Arg32Writer, Arg32Reader}
import geotrellis.process._

case class TileLayout(tileCols:Int, tileRows:Int, pixelCols:Int, pixelRows:Int)

trait TiledRasterData extends RasterData {
  def tileLayout:TileLayout

  def pixelCols:Int = tileLayout.pixelCols
  def pixelRows:Int = tileLayout.pixelRows
  def tileCols:Int = tileLayout.tileCols
  def tileRows:Int = tileLayout.tileRows

  def getTile(col:Int, row:Int):RasterData

  /**
   * Given an extent and resolution (RasterExtent), return the geographic
   * X-coordinates for each tile boundary in this raster data. For example,
   * if we have a 2x2 RasterData, with a raster extent whose X coordinates
   * span 13.0 - 83.0 (i.e. cellwidth is 35.0), we would return:
   *
   *   Array(13.0, 48.0, 83.0)
   *
   * Notice that if we have N columns of tiles we'll return N+1 Doubles.
   */
  def getXCoords(re:RasterExtent):Array[Double] = {
    val xs = Array.ofDim[Double](tileCols + 1)
    val cw = re.cellwidth
    val x1 = re.extent.xmin
    for (i <- 0 until tileCols) xs(i) = x1 + i * cw
    xs(tileCols) = re.extent.xmax
    xs
  }

  /**
   * This method is identical to getXCoords except that it functions on the
   * Y-axis instead.
   */
  def getYCoords(re:RasterExtent):Array[Double] = {
    val ys = Array.ofDim[Double](tileRows + 1)
    val ch = re.cellheight
    val y1 = re.extent.ymin
    for (i <- 0 until tileRows) ys(i) = y1 + i * ch
    ys(tileRows) = re.extent.ymax
    ys
  }

  /**
   * Given arrays of X and Y coordinates, and a particular tile location (c,r)
   * we will create the relevant geographic extent.
   */
  def getTileExtent(xs:Array[Double], ys:Array[Double], c:Int, r:Int) = {
    Extent(xs(c), ys(r), xs(c + 1), ys(r + 1))
  }

  def getTileRasterExtent(xs:Array[Double], ys:Array[Double],
                          re:RasterExtent, c:Int, r:Int) = {
    RasterExtent(getTileExtent(ys, xs, c, r), re.cellwidth, re.cellheight,
                 pixelCols, pixelRows)
  }

  def getTileRaster(xs:Array[Double], ys:Array[Double], re:RasterExtent, c:Int, r:Int) = {
    Raster(getTile(c, r), getTileRasterExtent(xs, ys, re, c, r))
  }

  def getTiles(re:RasterExtent): Array[Raster] = {
    val xs = getXCoords(re)
    val ys = getYCoords(re)
    val tiles = Array.ofDim[Raster](tileCols * tileRows)
    for (r <- 0 until tileRows; c <- 0 until tileCols) {
      tiles(r * tileCols + c) = getTileRaster(xs, ys, re, c, r)
    }
    tiles
  }

  def getTileList(re:RasterExtent): List[Raster] = {
    val xs = getXCoords(re)
    val ys = getYCoords(re)
    var tiles:List[Raster] = Nil
    for (r <- 0 until tileRows; c <- 0 until tileCols) {
      tiles = getTileRaster(xs, ys, re, c, r) :: tiles
    }
    tiles
  }

  def cellToTile(col:Int, row:Int) = (col / pixelCols, row / pixelRows)

  def copy = this

  def length:Int = {
    val n = lengthLong
    if (n > 2147483647L) sys.error("length won't fint in an integer")
    n.toInt
  }

  override def lengthLong:Long = {
    tileCols.toLong * pixelCols.toLong * tileRows.toLong * pixelCols.toLong
  }

  def combine2(other:RasterData)(f:(Int,Int) => Int):LazyTiledRasterData = {
    val t = LazyTiledWrapper(other, tileLayout)
    LazyTiledCombine2(this, t, f)
  }
  def foreach(f: Int => Unit) = {
    for (c <- 0 until tileCols; r <- 0 until tileRows)
      getTile(c, r).foreach(f)
  }
  def map(f: Int => Int):LazyTiledRasterData = LazyTiledMap(this, f)
  def mapIfSet(f:Int => Int):LazyTiledRasterData = LazyTiledMapIfSet(this, f)

  def force:StrictRasterData = sys.error("fixme")
  def asArray:ArrayRasterData = sys.error("fixme")

  def get(x:Int, y:Int, cols:Int) = {
    val (tx, ty) = cellToTile(x, y)
    val px = x % pixelCols
    val py = y % pixelRows
    getTile(tx, ty).get(px, py, pixelCols)
  }
  def set(x:Int, y:Int, z:Int, cols:Int) = sys.error("immutable")

  def getDouble(x:Int, y:Int, cols:Int) = {
    val (tx, ty) = cellToTile(x, y)
    getTile(tx, ty).getDouble(x, y, pixelCols)
  }
  def setDouble(x:Int, y:Int, z:Double, cols:Int) = sys.error("immutable")

}

case class TileSetRasterData(basePath:String, name:String,
                             tileLayout:TileLayout,
                             server:Server) extends TiledRasterData {
  def alloc(size:Int) = IntArrayRasterData.ofDim(size) //fixme
  def getType = TypeInt
  def getTile(col:Int, row:Int) = {
    val path = Tiler.tilePath(basePath, name, col, row)
    server.loadRaster(path).data
  }
}

case class TileArrayRasterData(tiles:Array[Raster],
                               tileLayout:TileLayout) extends TiledRasterData{
  val typ = tiles(0).data.getType
  def alloc(size:Int) = RasterData.allocByType(typ, size)
  def getType = typ
  def getTile(col:Int, row:Int) = tiles(row * tileCols + col).data
}

object LazyTiledWrapper {
  def apply(data:RasterData, tileLayout:TileLayout):TiledRasterData = {
    data match {
      case t:TiledRasterData => t // FIXME
      case a:ArrayRasterData => LazyTiledWrapper(a, tileLayout)
      case o => sys.error("explode")
    }
  }
}

case class LazyTiledWrapper(data:ArrayRasterData,
                            tileLayout:TileLayout) extends TiledRasterData {
  def alloc(size:Int) = data.alloc(size)
  def getType = data.getType
  def getTile(col:Int, row:Int) = {
    val col1 = pixelCols * col
    val row1 = pixelRows * row
    val col2 = col1 + pixelCols
    val row2 = row1 + pixelRows
    // FIXME raster data needs to store rows/cols
    LazyViewWrapper(data, tileCols * pixelCols, tileRows * pixelRows, col1, row1, col2, row2)
  }
}

case class LazyViewWrapper(data:ArrayRasterData, cols:Int, rows:Int,
                           col1:Int, row1:Int,
                           col2:Int, row2:Int) extends LazyRasterData {
  def copy = this

  def getType = data.getType
  def alloc(size:Int) = data.alloc(size)
  def length = (col2 - col1) * (row2 - row1)
  def apply(i:Int) = {
    val baseRow = i / cols
    val baseCol = i % cols
    val underlyingRow = row1 + baseRow
    val underlyingCol = col1 + baseCol
    data.apply(underlyingRow * cols + underlyingCol)
  }
  def toArray = force.toArray
}

trait LazyTiledRasterData extends TiledRasterData {
  def data:TiledRasterData

  def tileLayout = data.tileLayout

  def getType = data.getType
  def alloc(size:Int) = data.alloc(size)
  override def lengthLong = data.lengthLong
}

case class LazyTiledMap(data:TiledRasterData, g:Int => Int) extends LazyTiledRasterData {
  def getTile(col:Int, row:Int) = data.getTile(col, row).map(g)

  override def map(f:Int => Int) = LazyTiledMap(data, z => f(g(z)))
}

case class LazyTiledMapIfSet(data:TiledRasterData, g:Int => Int) extends LazyTiledRasterData {
  def gIfSet(z:Int) = if (z == NODATA) NODATA else g(z)

  def getTile(col:Int, row:Int) = data.getTile(col, row).mapIfSet(g)

  override def map(f:Int => Int) = LazyTiledMap(data, z => f(gIfSet(z)))
  override def mapIfSet(f:Int => Int) = LazyTiledMapIfSet(this, z => f(g(z)))
}

case class LazyTiledCombine2(data1:TiledRasterData, data2:TiledRasterData, g:(Int, Int) => Int)
extends LazyTiledRasterData {
  def data = data1 // FIXME this is kind of a hack

  val typ = RasterData.largestType(data1, data2)

  override def getType = typ
  override def alloc(size:Int) = RasterData.allocByType(typ, size)

  def getTile(col:Int, row:Int) = {
    val r1 = data1.getTile(col, row)
    val r2 = data2.getTile(col, row)
    r1.combine2(r2)(g)
  }

  override def map(f:Int => Int) = {
    LazyTiledCombine2(data1, data2, (a, b) => f(g(a, b)))
  }
  override def mapIfSet(f:Int => Int) = {
    def h(a:Int, b:Int) = {
      val z = g(a, b)
      if (z != NODATA) f(z) else NODATA
    }
    LazyTiledCombine2(data1, data2, h)
  }
}





/**
 * Used to create a tileset (TileRasterData) from a source raster.
 */
object Tiler {
  def tileName(name:String, col:Int, row:Int) = {
    "%s_%d_%d".format(name, col, row)
  }

  def tilePath(path:String, name:String, col:Int, row:Int) = {
    "%s/%s_%d_%d.arg".format(path, name, col, row)
  }
  
  def createTiledRaster(src:Raster, pixelCols:Int, pixelRows:Int) = {
    val data = createTiledRasterData(src, pixelCols, pixelRows)
    Raster(data, src.rasterExtent)
  }

  def createTiledRasterData(src:Raster, pixelCols:Int, pixelRows:Int) = {
    val re = src.rasterExtent
    val e = re.extent

    val tileCols = (re.cols + pixelCols - 1) / pixelCols
    val tileRows = (re.rows + pixelRows - 1) / pixelRows

    val cw = re.cellwidth
    val ch = re.cellheight

    val rasters = Array.ofDim[Raster](tileCols * tileRows)

    for (ty <- 0 until tileRows; tx <- 0 until tileCols) yield {
      // Note that since tile (0,0) is in the upper-left corner of our map,
      // we need to fix xmin and ymax to e.xmin and e.ymax. This asymmetry
      // will seem strange until you consider this fact.
      val xmin = e.xmin + (cw * tx * pixelCols)
      val ymax = e.ymax - (ch * ty * pixelRows)
      val xmax = xmin + (cw * pixelCols)
      val ymin = ymax - (cw * pixelRows)

      val tileExtent = Extent(xmin, ymin, xmax, ymax)
      val rasterExtent = RasterExtent(tileExtent, cw, ch, pixelCols, pixelRows)
      val data = Array.ofDim[Int](pixelCols * pixelRows)

      // TODO: if this code ends up being a performance bottleneck, we should
      // refactor away from using raster.get and for-comprehensions.
      for (y <- 0 until pixelRows; x <- 0 until pixelCols) {
        val xsrc = tx * pixelCols + x
        val ysrc = ty * pixelRows + y
        data(y * pixelCols + x) = if (xsrc >= re.cols || ysrc >= re.rows)
          NODATA else src.get(xsrc, ysrc)
      }
      rasters(ty * tileCols + tx) = Raster(data, rasterExtent)
    }

    val layout = TileLayout(tileCols, tileRows, pixelCols, pixelRows)
    TileArrayRasterData(rasters, layout)
  }
  
  def writeTiles(data:TiledRasterData, re:RasterExtent, name:String, path:String) = {
    val tiles = data.getTiles(re)
    for (row <- 0 until data.tileRows; col <- 0 until data.tileCols) {  
      val i = row * data.tileCols + col
      val r = tiles(i)
      val name2 = tileName(name, col, row)
      val path2 = tilePath(path, name, col, row)
      Arg32Writer.write(path2, r, name2)
    }
  }
  
  def deleteTiles(tiles:TiledRasterData, name:String, path:String) {
    for (row <- 0 until tiles.tileRows; col <- 0 until tiles.tileCols) {  
      val f = new java.io.File(tilePath(path,name,col,row))
      try {
        f.delete
      } catch {
        case e:Exception => {}
      }
    }
  }
}
