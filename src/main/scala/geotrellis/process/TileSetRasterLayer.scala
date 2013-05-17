package geotrellis.process

import geotrellis._
import geotrellis.util._
import geotrellis.data.arg.ArgReader
import geotrellis.raster.{TileSetRasterData,TileLayout,Tiler}

import com.typesafe.config.Config

object TileSetRasterLayerBuilder
extends RasterLayerBuilder {
  def apply(jsonPath:String, json:Config, cache:Option[Cache]):Option[RasterLayer] = {
    val tileDirPath = 
      if(json.hasPath("path")) {
        json.getString("path")
      } else {
        // Default to a directory with the same base name as the .json file.
        Filesystem.basename(jsonPath)
      }

    if(!new java.io.File(tileDirPath).isDirectory) {
      System.err.println(s"[ERROR] Raster in catalog points Tile Direcotry '$tileDirPath', but this is not a valid directory.")
      System.err.println("[ERROR]   Skipping this raster layer...")
      None
    } else {
      val layoutCols = json.getInt("layout_cols")
      val layoutRows = json.getInt("layout_rows")
      val pixelCols = json.getInt("pixel_cols")
      val pixelRows = json.getInt("pixel_rows")
      val cols = layoutCols * pixelCols
      val rows = layoutRows * pixelRows

      val (cw,ch) = getCellWidthAndHeight(json)

      val rasterExtent = RasterExtent(getExtent(json), cw, ch, cols, rows)
      val layout = TileLayout(layoutCols, layoutRows, pixelCols, pixelRows)

      val info = RasterLayerInfo(getName(json),
                                 getRasterType(json),
                                 rasterExtent,
                                 getEpsg(json),
                                 getXskew(json),
                                 getYskew(json))

      Some(new TileSetRasterLayer(info,tileDirPath,layout,cache))
    }
  }
}

class TileSetRasterLayer(info:RasterLayerInfo, 
                         val tileDirPath:String,
                         val tileLayout:TileLayout,
                         c:Option[Cache])
extends RasterLayer(info,c) {
  val loader = new TileLoader(tileDirPath,info,tileLayout)

  def getRaster(targetExtent:Option[RasterExtent]) = {
    Raster(TileSetRasterData(tileDirPath,
                             info.name,
                             info.rasterType,
                             tileLayout,
                             loader), info.rasterExtent) // TODO: Manage different raster extents.
  }

  def cache() = {}
}

class TileLoader(tileDirPath:String,tileSetInfo:RasterLayerInfo,tileLayout:TileLayout) {
  val resLayout = tileLayout.getResolutionLayout(tileSetInfo.rasterExtent)

  def getTile(col:Int,row:Int) = {
    val path = Tiler.tilePath(tileDirPath, tileSetInfo.name, col, row)
    val ext = resLayout.getRasterExtent(col,row)
    new ArgReader(path).readPath(tileSetInfo.rasterType,ext,ext)
  }
}
