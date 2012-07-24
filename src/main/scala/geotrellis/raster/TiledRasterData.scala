package geotrellis.raster

import geotrellis._
import geotrellis.process._
import geotrellis.data.arg.ArgWriter

/**
 * This class stores the layout of a tiled raster: the number of tiles (in
 * cols/rows) and also the size of each tile (in cols/rows of pixels).
 */
case class TileLayout(tileCols:Int, tileRows:Int, pixelCols:Int, pixelRows:Int) {

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
    for (i <- 0 until tileCols) xs(i) = x1 + i * cw * pixelCols
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
    for (i <- 0 until tileRows) ys(i) = y1 + i * ch * pixelRows
    ys(tileRows) = re.extent.ymax
    ys
  }

  /**
   * Given a particular RasterExtent (geographic area plus resolution) for the
   * entire tiled raster, construct an ResolutionLayout which will manage the
   * appropriate geographic boundaries, and resolution information, for each
   * tile.
   */
  def getResolutionLayout(re:RasterExtent) = {
    val xs = getXCoords(re)
    val ys = getYCoords(re)
    ResolutionLayout(xs, ys, re.cellwidth, re.cellheight, pixelCols, pixelRows)
  }
}


/**
 * For a particular resolution and tile layout, this class stores the
 * geographical boundaries of each tile extent.
 */
case class ResolutionLayout(xs:Array[Double], ys:Array[Double],
                        cw:Double, ch:Double, pcols:Int, prows:Int) {

  def getExtent(c:Int, r:Int) = Extent(xs(c), ys(r), xs(c + 1), ys(r + 1))

  def getRasterExtent(c:Int, r:Int) = {
    RasterExtent(getExtent(c, r), cw, ch, pcols, prows)
  }
}

/**
 *
 */
trait TiledRasterData extends RasterData {
  /**
   * Returns the particular layout of this TiledRasterData's tiles.
   */
  def tileLayout:TileLayout

  def pixelCols:Int = tileLayout.pixelCols
  def pixelRows:Int = tileLayout.pixelRows
  def tileCols:Int = tileLayout.tileCols
  def tileRows:Int = tileLayout.tileRows

  /**
   * Get a RasterData instance for a particular tile.
   */
  def getTile(col:Int, row:Int):RasterData

  /**
   * Given a resolution layout, and a particular tile's column/row, return a
   * Raster corresponding to that tile.
   */
  def getTileRaster(rl:ResolutionLayout, c:Int, r:Int) = {
    Raster(getTile(c, r), rl.getRasterExtent(c, r))
  }

  // TODO: remove one of getTiles or getTilesList.
  // TODO: maybe remove both, since normally we'll use tile operations.

  /**
   * Given a raster extent (geographical area plus resolution) return an array
   * of Rasters which represent that area as tiles.
   */
  def getTiles(re:RasterExtent): Array[Raster] = {
    val rl = tileLayout.getResolutionLayout(re)
    val tiles = Array.ofDim[Raster](tileCols * tileRows)
    for (r <- 0 until tileRows; c <- 0 until tileCols) {
      tiles(r * tileCols + c) = getTileRaster(rl, c, r)
    }
    tiles
  }

  /**
   * Function like getTiles but returns a list instead.
   */
  def getTileList(re:RasterExtent): List[Raster] = {
    val rl = tileLayout.getResolutionLayout(re)
    var tiles:List[Raster] = Nil
    for (r <- 0 until tileRows; c <- 0 until tileCols) {
      tiles = getTileRaster(rl, c, r) :: tiles
    }
    tiles
  }

  /**
   * Return an operation that when run will yield a particular tile's raster.
   */
  def getTileOp(rl:ResolutionLayout, c:Int, r:Int):Op[Raster]
 
  /**
  * Return a list of operations; each operation will load a tile's raster.
   */
  def getTileOpList(re:RasterExtent): List[Op[Raster]] = {
    val rl = tileLayout.getResolutionLayout(re)
    var tiles:List[Op[Raster]] = Nil
    for (r <- 0 until tileRows; c <- 0 until tileCols) {
      tiles = getTileOp(rl, c, r) :: tiles
    }
    tiles
  }

  /**
   * For a given grid coordinate, return the applicable tile coordinate.
   *
   * For example, given a 400x400 raster made up of 100x100 tiles,
   * cellToTile(144, 233) would return (1, 2).
   */
  def cellToTile(col:Int, row:Int) = (col / pixelCols, row / pixelRows)

  def convert(typ:RasterType):RasterData = sys.error("can't convert tiled data")

  def copy = this

  def length:Int = {
    val n = lengthLong
    if (n > 2147483647L) sys.error("length won't fit in an integer")
    n.toInt
  }

  override def lengthLong:Long = {
    tileCols.toLong * pixelCols.toLong * tileRows.toLong * pixelCols.toLong
  }

  def map(f:Int => Int):TiledRasterData = LazyTiledMap(this, f)

  def mapIfSet(f:Int => Int):TiledRasterData = LazyTiledMapIfSet(this, f)

  def combine(other:RasterData)(f:(Int,Int) => Int) = {
    LazyTiledCombine(this, LazyTiledWrapper(other, tileLayout), f)
  }

  // NOTE: not parallel. should do foreach on tiles in parallel when possible.
  def foreach(f: Int => Unit) = {
    for (c <- 0 until tileCols; r <- 0 until tileRows)
      getTile(c, r).foreach(f)
  }

  def mapDouble(f:Double => Double):TiledRasterData = LazyTiledMap(this, z => f(z).toInt)

  def mapIfSetDouble(f:Double => Double):TiledRasterData = LazyTiledMapIfSet(this, z => f(z).toInt)

  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = {
    LazyTiledCombine(this, LazyTiledWrapper(other, tileLayout), (z1,z2) => f(z1,z2).toInt)
  }

  def foreachDouble(f: Double => Unit) = {
    for (c <- 0 until tileCols; r <- 0 until tileRows)
      getTile(c, r).foreach(z => f(z))
  }

  // TODO: ideally the asArray method would be removed from RasterData
  def asArray = {
    if (lengthLong > 2147483647L) None
    val len = length
    val d = IntArrayRasterData.ofDim(len)
    var i = 0
    foreach { z => d(i) = z; i += 1 }
    Some(d)
  }

  // TODO: fix, maybe?
  def force = mutable

  // TODO: fix, maybe?
  def mutable:Option[MutableRasterData] = None

  def get(col:Int, row:Int, cols:Int) = {
    val tcol = col / pixelCols
    val trow = row / pixelRows
    val pcol = col % pixelCols
    val prow = row % pixelRows
    getTile(tcol, trow).get(pcol, prow, pixelCols)
  }

  def getDouble(col:Int, row:Int, cols:Int) = {
    val tcol = col / pixelCols
    val trow = row / pixelRows
    val pcol = col % pixelCols
    val prow = row % pixelRows
    getTile(tcol, trow).getDouble(pcol, prow, pixelCols)
  }
}


/**
 * This RasterData uses no in-memory caching and loads all tile data from disk
 * when needed.
 *
 * Currently this is the only TileRasterData that can be reliably used on data
 * too large to fit in memory.
 */
case class TileSetRasterData(basePath:String, name:String, typ:RasterType,
                             tileLayout:TileLayout,
                             server:Server) extends TiledRasterData {
  def getType = typ
  def alloc(size:Int) = RasterData.allocByType(typ, size)

  def getTile(col:Int, row:Int) = {
    val path = Tiler.tilePath(basePath, name, col, row)
    server.loadRaster(path).data match {
      case a:ArrayRasterData => LazyArrayWrapper(a)
      case o => o
    }
  }

  override def getTileOp(rl:ResolutionLayout, c:Int, r:Int) = {
    val path = Tiler.tilePath(basePath, name, c, r)
    logic.Do(io.LoadFile(path))(_.defer)    
  }  
}


/**
 * This RasterData wraps an array of tile Rasters in memory. It is the fastest
 * of the TileRasterData classes but requires all the tiles to be loaded as
 * Rasters.
 */
case class TileArrayRasterData(tiles:Array[Raster],
                               tileLayout:TileLayout) extends TiledRasterData{
  val typ = tiles(0).data.getType
  def alloc(size:Int) = RasterData.allocByType(typ, size)
  def getType = typ
  def getTile(col:Int, row:Int) = tiles(row * tileCols + col).data match {
    case a:ArrayRasterData => LazyArrayWrapper(a)
    case o => o
  }
  def getTileOp(rl:ResolutionLayout, c:Int, r:Int):Op[Raster] =
    Literal(getTileRaster(rl, c, r))
}

object LazyTiledWrapper {
  def apply(data:RasterData, tileLayout:TileLayout):TiledRasterData = {
    // TODO: ensure same tile layout
    data match {
      case t:TiledRasterData => t
      case a:ArrayRasterData => LazyTiledWrapper(a, tileLayout)
      case o => sys.error("explode")
    }
  }
}


/**
 * This RasterData warps an existing ArrayRasterData so as to allow operations
 * on it to be executed in parallel. The underlying RasterData is limited to
 * a single array's worth of data (~2.1G).
 */
case class LazyTiledWrapper(data:ArrayRasterData,
                            tileLayout:TileLayout) extends TiledRasterData {

  if (data.length != length) sys.error("oh no!")

  def getType = data.getType

  def alloc(size:Int) = data.alloc(size)

  def getTile(col:Int, row:Int) = {
    val col1 = pixelCols * col
    val row1 = pixelRows * row
    val col2 = col1 + pixelCols
    val row2 = row1 + pixelRows
    LazyViewWrapper(data, tileCols * pixelCols, tileRows * pixelRows,
                    col1, row1, col2, row2)
  }

  def getTileOp(rl:ResolutionLayout, c:Int, r:Int) = Literal(getTileRaster(rl, c, r))
}


/**
 * This RasterData class is used by LazyTiledWrapper to encompass a particular
 * chunk of an underlying ArrayRaster. For instance, a LazyViewWrapper might
 * represent a particular 256x256 chunk of underlying 4096x4096 raster.
 */
case class LazyViewWrapper(data:ArrayRasterData, cols:Int, rows:Int,
                           col1:Int, row1:Int,
                           col2:Int, row2:Int) extends LazyRasterData {
  val myCols = (col2 - col1)
  val myRows = (row2 - row1)
  val myLen = myCols * myRows

  final def getType = data.getType
  final def alloc(size:Int) = data.alloc(size)
  final def length = myLen
  final def copy = this

  final def translate(i:Int) = (row1 + i / myCols) * cols + (col1 + i % myCols)

  final def apply(i:Int) = data.apply(translate(i))

  override final def map(f:Int => Int) = LazyMap(this, f)

  override final def mapIfSet(f:Int => Int) = LazyMapIfSet(this, f)

  override final def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine(this, a, f)
    case o => o.combine(this)((z2, z1) => f(z1, z2))
  }

  override final def foreach(f:Int => Unit) = {
    var r = row1
    val rlimit = row2
    val climit = col2
    val width = cols
    while (r < rlimit) {
      var c = col1
      while (c < climit) {
        f(data(r * width + c))
        c += 1
      }
      r += 1
    }
  }

  final def applyDouble(i:Int) = data.applyDouble(translate(i))

  override final def mapDouble(f:Double => Double) = LazyMap(this, z => f(z).toInt)

  override final def mapIfSetDouble(f:Double => Double) = LazyMapIfSet(this, z => f(z).toInt)

  override final def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombine(this, a, (z1,z2) => f(z1,z2).toInt)
    case o => o.combine(this)((z2, z1) => f(z1, z2).toInt)
  }

  override final def foreachDouble(f:Double => Unit) = {
    var r = row1
    val rlimit = row2
    val climit = col2
    val width = cols
    while (r < rlimit) {
      var c = col1
      while (c < climit) {
        f(data(r * width + c))
        c += 1
      }
      r += 1
    }
  }
}


/**
 * This trait provides some methods in terms of underlying raster data.
 *
 * Note that all TiledRasterData objects perform operations like map lazily.
 * This trait is designed for classes who wrap another TiledRasterData and want
 * to forward some of their methods to the underlying data.
 */
trait LazyTiledRasterData extends TiledRasterData {
  def data:TiledRasterData

  def tileLayout = data.tileLayout

  def getType = data.getType
  def alloc(size:Int) = data.alloc(size)
  override def lengthLong = data.lengthLong
}


/**
 * This lazy, tiled raster data represents a map over a tiled raster data.
 */
case class LazyTiledMap(data:TiledRasterData, g:Int => Int) extends LazyTiledRasterData {
  def getTile(col:Int, row:Int) = data.getTile(col, row).map(g)

  override def getTileOp(rl:ResolutionLayout, c:Int, r:Int) = 
      logic.Do(data.getTileOp(rl, c, r))(_.map(g))

  override def map(f:Int => Int) = LazyTiledMap(data, z => f(g(z)))
}


/**
 * This lazy, tiled raster data represents a mapIfSet over a tiled raster data.
 */
case class LazyTiledMapIfSet(data:TiledRasterData, g:Int => Int) extends LazyTiledRasterData {
  def gIfSet(z:Int) = if (z == NODATA) NODATA else g(z)

  def getTile(col:Int, row:Int) = data.getTile(col, row).mapIfSet(g)

  override def getTileOp(rl:ResolutionLayout, c:Int, r:Int) =
    logic.Do(data.getTileOp(rl, c, r))(_.mapIfSet(g))

  override def map(f:Int => Int) = LazyTiledMap(data, z => f(gIfSet(z)))

  override def mapIfSet(f:Int => Int) = LazyTiledMapIfSet(this, z => f(g(z)))
}


/**
 * This lazy, tiled raster data represents two tiled raster data objects
 * combined with a function.
 */
case class LazyTiledCombine(data1:TiledRasterData, data2:TiledRasterData,
                             g:(Int, Int) => Int) extends TiledRasterData {
  val typ = RasterData.largestType(data1, data2)
  val layout = data1.tileLayout // TODO: must handle different tile layouts

  if (data1.lengthLong != data2.lengthLong)
    sys.error("invalid raster sizes: %s, %s" format (data1.lengthLong,
                                                     data2.lengthLong))

  def tileLayout = layout

  def getType = typ
  def alloc(size:Int) = RasterData.allocByType(typ, size)

  def getTile(col:Int, row:Int) = {
    val r1 = data1.getTile(col, row)
    val r2 = data2.getTile(col, row)
    r1.combine(r2)(g)
  }

  override def getTileOp(rl:ResolutionLayout, c:Int, r:Int) = {
    val r1 = data1.getTileOp(rl, c, r)
    val r2 = data2.getTileOp(rl, c, r)
    logic.Do(r1, r2)((r1,r2) => r1.combine(r2)(g))
  }

  override def map(f:Int => Int) = {
    LazyTiledCombine(data1, data2, (a, b) => f(g(a, b)))
  }

  override def mapIfSet(f:Int => Int) = {
    def h(a:Int, b:Int) = {
      val z = g(a, b)
      if (z != NODATA) f(z) else NODATA
    }
    LazyTiledCombine(data1, data2, h)
  }
}


/**
 * Used to create tiled rasters, as well as tilesets on the filesystem, based
 * on a source raster.
 *
 * These files (on disk) can be used by a TileSetRasterData, or loaded into an
 * array of rasters to be used by TileArrayRasterData.
 *
 * A tile set has a base path (e.g. "foo/bar") which is used along with the
 * "tile coordinates" (e.g. tile 0,4) to compute the path of each tile (in this
 * case "foo/bar_0_4.arg").
 */
object Tiler {
  /**
   * Given a name ("bar") a col (0), and a row (4), returns the correct name
   * for this tile ("bar_0_4").
   */
  def tileName(name:String, col:Int, row:Int) = {
    "%s_%d_%d".format(name, col, row)
  }

  /**
   * Given a path ("foo"), a name ("bar"), a col (0), and a row (4), returns
   * the correct name for this tile ("foo/bar_0_4").
   */
  def tilePath(path:String, name:String, col:Int, row:Int) = {
    "%s/%s_%d_%d.arg".format(path, name, col, row)
  }

  /**
   * From a raster, makes a new Raster (using an array of tiles in memory).
   */
  def createTiledRaster(src:Raster, pixelCols:Int, pixelRows:Int) = {
    val data = createTiledRasterData(src, pixelCols, pixelRows)
    Raster(data, src.rasterExtent)
  }
 
  def buildTileLayout(re:RasterExtent, pixelCols:Int, pixelRows:Int) = {
    val tileCols = (re.cols + pixelCols - 1) / pixelCols
    val tileRows = (re.rows + pixelRows - 1) / pixelRows
    TileLayout(tileCols, tileRows, pixelCols, pixelRows)
  }

  def buildTileRasterExtent(tx:Int, ty:Int, re:RasterExtent, pixelCols:Int, pixelRows:Int) = {
    val cw = re.cellwidth
    val ch = re.cellheight
    val e = re.extent
    // Note that since tile (0,0) is in the upper-left corner of our map,
    // we need to fix xmin and ymax to e.xmin and e.ymax. This asymmetry
    // will seem strange until you consider that fact.
    val xmin = e.xmin + (cw * tx * pixelCols)
    val ymax = e.ymax - (ch * ty * pixelRows)
    val xmax = xmin + (cw * pixelCols)
    val ymin = ymax - (cw * pixelRows)

    val te = Extent(xmin, ymin, xmax, ymax)
    val tre = RasterExtent(te, cw, ch, pixelCols, pixelRows)
    tre
  }

  /**
   * From a raster, makes a new TiledArrayRaster (an array of tiles in memory).
   */
  def createTiledRasterData(src:Raster, pixelCols:Int, pixelRows:Int) = {
    val re = src.rasterExtent
    val e = re.extent
  
    val layout = buildTileLayout(re, pixelCols, pixelRows)
    val tileCols = layout.tileCols
    val tileRows = layout.tileRows

    val cw = re.cellwidth
    val ch = re.cellheight

    val rasters = Array.ofDim[Raster](tileCols * tileRows)

    for (ty <- 0 until tileRows; tx <- 0 until tileCols) yield {
      val data = RasterData.allocByType(src.data.getType, pixelCols * pixelRows)

      // TODO: if this code ends up being a performance bottleneck, we should
      // refactor away from using raster.get and for-comprehensions.
      for (y <- 0 until pixelRows; x <- 0 until pixelCols) {
        val xsrc = tx * pixelCols + x
        val ysrc = ty * pixelRows + y
        val i = y * pixelCols + x
        data(i) = if (xsrc >= re.cols || ysrc >= re.rows) NODATA else src.get(xsrc, ysrc)
      }
      val tre = buildTileRasterExtent(tx,ty,re,pixelCols,pixelRows)
      rasters(ty * tileCols + tx) = Raster(data, tre)
    }

    TileArrayRasterData(rasters, layout)
  }

  /**
   * Write a TiledRasterData to disk as a tile set, using the provided path and
   * name to determine what filenames to use.
   */
  def writeTiles(data:TiledRasterData, re:RasterExtent, name:String, path:String) = {
    val tiles = data.getTiles(re)
    for (row <- 0 until data.tileRows; col <- 0 until data.tileCols) {  
      val i = row * data.tileCols + col
      val r = tiles(i)
      val name2 = tileName(name, col, row)
      val path2 = tilePath(path, name, col, row)

      ArgWriter(data.getType).write(path2, r, name2)
    }
  }

  /**
   * Write a TiledRasterData to disk as a tile set, creating each tile's data
   * by executing a function that returns a raster.
   *
   * Note that the function will need to generate its RasterExtent from the ResolutionLayout,
   * e.g.
   * val rl = tileLayout.getResolutionLayout(re)
   * val tileRasterExtent = rl.getRasterExtent(col, row)
   */ 
  def writeTilesFromFunction(pixelCols:Int, pixelRows:Int, re:RasterExtent, name:String, path:String,
    f:(Int,Int,TileLayout,RasterExtent) => Raster) {
    val layout = buildTileLayout(re, pixelCols, pixelRows)
    for (row <- 0 until layout.tileRows; col <- 0 until layout.tileCols) {
      val raster = f(col, row, layout, re)
      val name2 = tileName(name, col, row)
      val path2 = tilePath(path, name, col, row) 
      ArgWriter(raster.data.getType).write(path2, raster, name2)
    }
  }
 
  /**
   * Given a path and name, deletes the relevant tileset from the disk.
   */  
  def deleteTiles(tiles:TiledRasterData, name:String, path:String) {
    for (row <- 0 until tiles.tileRows; col <- 0 until tiles.tileCols) {  
      val f = new java.io.File(tilePath(path, name, col, row))
      try {
        f.delete
      } catch {
        case e:Exception => {}
      }
    }
  }
}
