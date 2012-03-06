package geotrellis.raster

import geotrellis._
import geotrellis.data.{Arg32Writer, Arg32Reader}
import geotrellis.process._

/**
 * Used to create a tileset (TileRasterData) from a source raster.
 */
object Tiler {
  def createTileRaster(src: IntRaster, pixels: Int) = {
    val srcExtent = src.rasterExtent
    val tileExtent = srcExtent
    val tileRasterData = createTileRasterData(src, pixels)
    IntRaster(tileRasterData, tileExtent)
  }

  def createTileRasterData(src: IntRaster, pixels: Int): TileRasterData = {
    val srcExtent = src.rasterExtent
    //val tileExtent = srcExtent

    val tileCols = (srcExtent.cols / pixels) + 1
    val tileRows = (srcExtent.rows / pixels) + 1

    val cellwidth = srcExtent.cellwidth
    val cellheight = srcExtent.cellheight

    val rasters = for (ty <- 0 until tileRows; tx <- 0 until tileCols) yield {
      // calculate extent of tile
      val xmin = srcExtent.extent.xmin + (srcExtent.cellwidth * (tx * pixels))
      val ymax = srcExtent.extent.ymax - (srcExtent.cellheight * (ty * pixels))
      val xmax = xmin + (srcExtent.cellwidth * pixels)
      val ymin = ymax - (srcExtent.cellwidth * pixels)

      val tileExtent = Extent(xmin, ymin, xmax, ymax)
      val rasterExtent = RasterExtent(tileExtent, srcExtent.cellwidth, srcExtent.cellheight, pixels, pixels);
      val data = Array.ofDim[Int](pixels * pixels)

      for (y <- 0 until pixels; x <- 0 until pixels) {
        val xsrc = tx * pixels + x
        val ysrc = ty * pixels + y
        data(y * pixels + x) = if (xsrc >= srcExtent.cols || ysrc >= srcExtent.rows)
          NODATA else src.get(xsrc, ysrc)
      }
      Some(IntRaster(data, rasterExtent))
    }
    val tileRasterData = TileRasterData(TileSet(srcExtent, pixels), rasters.toArray)
    tileRasterData
  }
  
  def tileName(name:String, col:Int, row:Int) = "%s_%d_%d".format(name, col, row)

  def tilePath(path:String, name:String, col:Int, row:Int) = {
    "%s/%s_%d_%d.arg32".format(path, name, col, row)
  }
  
  def writeTiles(tiles:TileRasterData, name:String, path:String) = {
    for (trow <- 0 until tiles.tileRows; tcol <- 0 until tiles.tileCols) {  
      tiles.rasters(trow * tiles.tileCols + tcol) match {
        case Some(r:IntRaster) => {
          val name2 = tileName(name, tcol, trow)
          val path2 = tilePath(path, name, tcol, trow)
          Arg32Writer.write(path2, r, name2)
        }
        case None => {}
      }
    }
  }
  
  def deleteTiles(tiles:TileRasterData, name:String, path:String) {
    for (trow <- 0 until tiles.tileRows; tcol <- 0 until tiles.tileCols) {  
      tiles.rasters(trow * tiles.tileCols + tcol) match {
        case Some(r:IntRaster) => tilePath(path,name,tcol,trow)
        case None => {}
      }
    }
  }
  
  def makeTileLoader(name:String, path:String, s:Server) = {
    (tx:Int,ty:Int) => {
      val path2 = tilePath(path, name, tx, ty)
      Some(Arg32Reader.read(path2, None, None))
    } 
  }
}

case class TileExtent(minCol: Int, minRow: Int, maxCol: Int, maxRow: Int) {
  def contains(col:Int, row:Int) = {
    col >= minCol && col <= maxCol && row >= minRow && row <= maxRow
  }
}

/**
 * A large raster that has been split into smaller rasters.
 */
case class TileSet(rasterExtent:RasterExtent, tileSize:Int) {
  val tileCols = (rasterExtent.cols / tileSize) + 1 // ceil(cols/pixels)
  val tileRows = (rasterExtent.rows / tileSize) + 1 // ceil(rows/pixels)

  def cellToTile(col:Int,row:Int) = ( col / tileSize, row / tileSize)
    
  /**
   * Returns a range of tiles for a given map extent.
   */
  def tileRange(mapExtent:Extent) = {
    // get col/row min and max of extent from full grid (according to rasterExtent)
    val sw = mapExtent.southWest
    val ne = mapExtent.northEast
    val (minCol, minRow) = rasterExtent.mapToGrid(sw)
    val (maxCol, maxRow) = rasterExtent.mapToGrid(ne)
    
    val (tMinCol, tMinRow) = cellToTile(minCol,minRow)
    val (tMaxCol, tMaxRow) = cellToTile(maxCol,maxRow)
    TileExtent(tMinCol, tMinRow, tMaxCol, tMaxRow)
  }

  
}

object TileRasterData {
  def apply(tileset: TileSet, loadExtent: Extent,
            loader: (Int, Int) => Option[IntRaster]): TileRasterData = {
    val tileExtent = tileset.tileRange(loadExtent);
    val rasters = for (y <- 0 until tileset.tileRows; x<- 0 until tileset.tileCols) yield {
      if (tileExtent.contains(x,y)) {
        loader(x,y)
      } else {
        None
      }
    }
    TileRasterData(tileset, rasters.toArray)
  }
}

/**
  * TileRasterData provides a data source that is backed by a grid of sub-rasters.
  */
case class TileRasterData(tileSet:TileSet, rasters:Array[Option[IntRaster]]) extends RasterData {
  val rasterExtent = tileSet.rasterExtent;
  val pixels = tileSet.tileSize;
  
  val tileCols = (rasterExtent.cols / pixels) + 1 // ceil(cols/pixels)
  val tileRows = (rasterExtent.rows / pixels) + 1 // ceil(rows/pixels)
  val tileCellwidth = rasterExtent.cellwidth / pixels
  val tileCellheight = rasterExtent.cellheight / pixels
    
  val tileExtent = RasterExtent(rasterExtent.extent, tileCellwidth, tileCellheight, tileCols, tileRows)

  /**
   * Get value at given coordinates.
   */
  def apply(i: Int):Int = {
    val row = i / rasterExtent.cols
    val col = i % rasterExtent.cols

    val c = col / pixels
    val r = row / pixels
    
    rasters(r * tileCols + c) map {_.get(col % pixels, row % pixels)} getOrElse NODATA
  }

  /**
    * Set value at given coordinates.  (valid for tile raster??)
    */
  def update(i:Int, value:Int) {
    val row = i / rasterExtent.cols
    val col = i % rasterExtent.cols
    val c = col / pixels
    val r = row / pixels
    rasters(r * tileCols + c).get.set(c % pixels, r % pixels, value)
  }
  
  /**
    * Copy: not implemented
    */
  def copy:RasterData = { this }

  def length:Int = { rasterExtent.cols * rasterExtent.rows }

  def asArray():Array[Int] = {
    throw new Exception("not implemented yet");
    return null;
  }
}
// vim: set ts=4 sw=4 et:
