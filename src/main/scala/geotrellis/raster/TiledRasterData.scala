package geotrellis.raster

import geotrellis._
import geotrellis.util.Filesystem
import geotrellis.process._
import geotrellis.data.arg.{ArgWriter,ArgReader}
import geotrellis.data.Gdal
import geotrellis.feature.Polygon
import java.io.{FileOutputStream, BufferedOutputStream}
import geotrellis.util.Filesystem

/**
 *
 */
trait TiledRasterData extends RasterData with Serializable {
  /**
   * Returns the particular layout of this TiledRasterData's tiles.
   */
  def tileLayout:TileLayout
  
  override def tileLayoutOpt = Some(tileLayout)

  override def isTiled = true
  override def isLazy  = true

  def cols = tileLayout.totalCols
  def rows = tileLayout.totalRows

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
  def getTileRaster(rl:ResolutionLayout, c:Int, r:Int):Raster = {
    Raster(getTile(c, r), rl.getRasterExtent(c, r))
  }

  /**
   * Given a raster extent (geographical area plus resolution) return an array
   * of Rasters which represent that area as tiles.
   */
  def getTiles(re:RasterExtent): List[Raster] = {
    val rl = tileLayout.getResolutionLayout(re)
    (for (r <- 0 until tileRows; 
         c <- 0 until tileCols) yield { getTileRaster(rl, c, r) })
    .toList
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
    (for (r <- 0 until tileRows; 
         c <- 0 until tileCols) yield { getTileOp(rl, c, r) })
    .toList
  }

  /**
   * Return a list of operations that load a tile's raster, filtered by a clip extent.
   * If any tile does not touch the clip extent, it will not be included in the list. 
   *
   * @param re          RasterExtent of this tileset
   * @param clipExtent  Polygon to be used to determine which tiles to return 
   */
  def getTileOpList(re:RasterExtent, clipExtent:Polygon[_]):List[Op[Raster]] = {
    val rl:ResolutionLayout = tileLayout.getResolutionLayout(re)
  
    var tiles:List[Op[Raster]] = Nil
    for (r <- 0 until tileRows; c <- 0 until tileCols) {

      val tilePolygon  = rl.getRasterExtent(c,r)
                           .extent.asFeature()

      if (tilePolygon.geom.intersects(clipExtent.geom)) {
        tiles = getTileOp(rl, c, r) :: tiles
      }
    }
    tiles
  }

  /**
   * For a given grid coordinate, return the applicable tile coordinate.
   *
   * For example, given a 400x400 raster made up of 100x100 tiles,
   * cellToTile(144, 233) would return (1, 2).
   */
  def cellToTile(col:Int, row:Int):(Int,Int) = (col / pixelCols, row / pixelRows)

  def convert(typ:RasterType):RasterData = sys.error("can't convert tiled data")

  def copy = this

  def length:Int = {
    val n = lengthLong
    if (n > 2147483647L) sys.error("length won't fit in an integer")
    n.toInt
  }

  override def lengthLong:Long = {
    tileCols.toLong * pixelCols.toLong * tileRows.toLong * pixelRows.toLong
  }

  def map(f:Int => Int):TiledRasterData = LazyTiledMap(this, f)

  def mapIfSet(f:Int => Int):TiledRasterData = LazyTiledMapIfSet(this, f)

  def combine(other:RasterData)(f:(Int,Int) => Int) = 
    LazyTiledCombine(this, LazyTiledWrapper(other, tileLayout), f)

  // NOTE: not parallel. should do foreach on tiles in parallel when possible.
  def foreach(f: Int => Unit) = {
    for (c <- 0 until tileCols; r <- 0 until tileRows)
      getTile(c, r).foreach(f)
  }

  def mapDouble(f:Double => Double):TiledRasterData = 
    LazyTiledMap(this, z => f(z).toInt)

  def mapIfSetDouble(f:Double => Double):TiledRasterData = 
    LazyTiledMapIfSet(this, z => f(z).toInt)

  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = 
    LazyTiledCombine(this, LazyTiledWrapper(other, tileLayout), (z1,z2) => f(z1,z2).toInt)

  def foreachDouble(f: Double => Unit) = {
    for (c <- 0 until tileCols; r <- 0 until tileRows)
      getTile(c, r).foreach(z => f(z))
  }

  def asArray = {
    if (lengthLong > 2147483647L) None
    val len = length
    val d = IntArrayRasterData.ofDim(cols, rows)
    for( r <- 0 until rows; c <- 0 until cols) {
      d(r * cols + c) = get(c,r)
    }
    Some(d)
  }

  def force = mutable

  def mutable:Option[MutableRasterData] = asArray

  def get(col:Int, row:Int) = {
    val tcol = col / pixelCols
    val trow = row / pixelRows
    val pcol = col % pixelCols
    val prow = row % pixelRows
    getTile(tcol, trow).get(pcol, prow)
  }

  def getDouble(col:Int, row:Int) = {
    val tcol = col / pixelCols
    val trow = row / pixelRows
    val pcol = col % pixelCols
    val prow = row % pixelRows
    getTile(tcol, trow).getDouble(pcol, prow)
  }
  
  /***
   * Returns true if (col,row) is a valid tile in this tileset.
   * 
   * If the column or row is negative or higher than the number of columns or rows,
   * returns false.
   */
  def withinBounds(col:Int,row:Int):Boolean = 
    (col >= 0 && col < cols && row >= 0 && row < rows)

}

/**
 * This RasterData uses no in-memory caching and loads all tile data from disk
 * when needed.
 *
 * Currently this is the only TileRasterData that can be reliably used on data
 * too large to fit in memory.
 */
case class TileSetRasterData(basePath:String, 
                             name:String, 
                             typ:RasterType, 
                             tileLayout:TileLayout,
                             loader:TileLoader) extends TiledRasterData {
  def getType = typ
  def alloc(cols:Int, rows:Int) = RasterData.allocByType(typ, cols, rows)

  def getTile(col:Int, row:Int) = {
    loader.getTile(col,row).data match {
      case i:IntConstant => i
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
class TileArrayRasterData(val tiles:Array[Raster],
                          val tileLayout:TileLayout,
                          val rasterExtent:RasterExtent) extends TiledRasterData  with Serializable {
  val typ = tiles(0).data.getType
  def alloc(cols:Int, rows:Int) = RasterData.allocByType(typ, cols, rows)
  def getType = typ
  def getTile(col:Int, row:Int) = tiles(row * tileCols + col).data match {
    case i:IntConstant => i
    case a:ArrayRasterData => LazyArrayWrapper(a)
    case o => o
  }
  def getTileOp(rl:ResolutionLayout, c:Int, r:Int):Op[Raster] =
    Literal(getTileRaster(rl, c, r))
}

object TileArrayRasterData {
  def apply(tiles:Array[Raster], tileLayout:TileLayout, rasterExtent:RasterExtent) = 
    new TileArrayRasterData(tiles, tileLayout, rasterExtent)
  /**
   * Create a TileArrayRasterData by loading all tiles from disk.
   */
  def apply(basePath:String, name:String, typ:RasterType, tileLayout:TileLayout, 
      rasterExtent:RasterExtent, server:Server) = {
    val rl = tileLayout.getResolutionLayout(rasterExtent)
    
    var tiles:List[Raster] = Nil
    for (r <- 0 until tileLayout.tileRows; c <- 0 until tileLayout.tileCols) {
      val path = Tiler.tilePath(basePath, name, c, r)
      tiles = tiles ::: List(server.getRaster(path,None,None))
    }
    new TileArrayRasterData(tiles.toArray, tileLayout, rasterExtent)
  }
} 

object LazyTiledWrapper {
  def apply(data:RasterData, tileLayout:TileLayout):TiledRasterData = {
    // TODO: ensure same tile layout
    data match {
      case t:TiledRasterData => t
      case a:ArrayRasterData => new LazyTiledWrapper(a, tileLayout)
      case o => sys.error("RasterData must be TiledRasterData or ArrayRasterData")
    }
  }
}


/**
 * This RasterData wraps an existing ArrayRasterData so as to allow operations
 * on it to be executed in parallel. The underlying RasterData is limited to
 * a single array's worth of data (~2.1G).
 */
class LazyTiledWrapper(data:ArrayRasterData,
                       val tileLayout:TileLayout) extends TiledRasterData {

  if (data.length != length) sys.error(s"Data length ${data.length} does not match tiled raster data length ${length}.")

  def getType = data.getType

  def alloc(cols:Int, rows:Int) = data.alloc(cols, rows)

  def getTile(col:Int, row:Int) = {
    val col1 = pixelCols * col
    val row1 = pixelRows * row
    val col2 = col1 + pixelCols
    val row2 = row1 + pixelRows
    LazyViewWrapper(data, tileCols * pixelCols,
                    col1, row1, col2, row2)
  }

  def getTileOp(rl:ResolutionLayout, c:Int, r:Int) = Literal(getTileRaster(rl, c, r))
}

/**
 * This RasterData class is used by LazyTiledWrapper to encompass a particular
 * chunk of an underlying ArrayRaster. For instance, a LazyViewWrapper might
 * represent a particular 256x256 chunk of underlying 4096x4096 raster.
 */
case class LazyViewWrapper(data:ArrayRasterData,
                           parentCols:Int,
                           col1:Int, row1:Int,
                           col2:Int, row2:Int) extends LazyRasterData {
  val cols = (col2 - col1)
  val rows = (row2 - row1)

  final def getType = data.getType
  final def alloc(cols:Int, rows:Int) = data.alloc(cols, rows)
  final def length = cols * rows
  final def copy = this

  final def translate(i:Int) = (row1 + i / cols) * parentCols + (col1 + i % cols)

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
    val width = parentCols
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
    val width = parentCols
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
  def alloc(cols:Int, rows:Int) = data.alloc(cols, rows)
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

  if (data1.lengthLong != data2.lengthLong) {
    val size1 = s"${data1.cols} x ${data1.rows}"
    val size2 = s"${data2.cols} x ${data2.rows}"
    sys.error(s"Cannot combine rasters of different sizes: $size1 vs $size2")
  }

  def tileLayout = layout

  def getType = typ
  def alloc(cols:Int, rows:Int) = RasterData.allocByType(typ, cols, rows)

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
