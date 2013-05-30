package geotrellis.process

import geotrellis._
import geotrellis.util._
import geotrellis.data.arg.ArgReader
import geotrellis.raster.{TileSetRasterData,
                          TileLayout,
                          Tiler,
                          CroppedRaster,
                          IntConstant}

import com.typesafe.config.Config
import java.io.File

object TileSetRasterLayerBuilder
extends RasterLayerBuilder {
  def apply(jsonPath:String, json:Config, cache:Option[Cache]):Option[RasterLayer] = {
    val tileDir = 
      if(json.hasPath("path")) {
        val f = new File(json.getString("path"))
        if(f.isAbsolute) {
          f
        } else {
          new File(new File(jsonPath).getParent, f.getPath)
        }
      } else {
        // Default to a directory with the same base name as the .json file.
        new File(new File(jsonPath).getParent, getName(json))
      }

    if(!tileDir.isDirectory) {
      System.err.println(s"[ERROR] Raster in catalog points Tile Directory '${tileDir.getPath}', but this is not a valid directory.")
      System.err.println("[ERROR]   Skipping this raster layer...")
      None
    } else {
      val tileDirPath = tileDir.getPath
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
  def getRaster(targetExtent:Option[RasterExtent]) = {
    targetExtent match {
      case Some(re) =>
        val r = Raster(getData(Some((re.cellwidth,re.cellheight))), info.rasterExtent)
        println(s"Cropping ${r.rasterExtent.extent} to ${re.extent}")
        CroppedRaster(r,re.extent)
      case None => Raster(getData(None), info.rasterExtent)
    }
  }

  def getData(cellSize:Option[(Double,Double)] = None) = 
    TileSetRasterData(tileDirPath,
                      info.name,
                      info.rasterType,
                      tileLayout,
                      getTileLoader(cellSize))

  private def getTileLoader(cellSize:Option[(Double,Double)] = None) =
    new TileLoader(tileDirPath,info,tileLayout,cellSize)

  def cache() = {} // TODO: Implement
}

class TileLoader(tileDirPath:String,
                 tileSetInfo:RasterLayerInfo,
                 tileLayout:TileLayout,
                 cellSize:Option[(Double,Double)] = None) {
  val resLayout = tileLayout.getResolutionLayout(tileSetInfo.rasterExtent)

  val rasterExtent = tileSetInfo.rasterExtent

  cellSize match {
    case Some((cellWidth,cellHeight)) =>
      println(s"Loading tiles with $cellWidth and $cellHeight")
    case None => 
      println(s"Loading tiles with normal resolution.")
  }

  def getTile(col:Int,row:Int):Raster = {
    val re = resLayout.getRasterExtent(col,row)
    val te =
      cellSize match {
        case Some((cellWidth,cellHeight)) =>
          re.withResolution(cellWidth,cellHeight)
        case None => re
      }

    if(col < 0 || row < 0 ||
       tileLayout.tileCols <= col || tileLayout.tileRows <= row) {
      Raster(IntConstant(NODATA, rasterExtent.cols, rasterExtent.rows), te)
    } else {
      val path = Tiler.tilePath(tileDirPath, tileSetInfo.name, col, row)
      val te =
        cellSize match {
          case Some((cellWidth,cellHeight)) =>
            re.withResolution(cellWidth,cellHeight)
          case None => re
        }
      new ArgReader(path).readPath(tileSetInfo.rasterType,re,te)
    }
  }
}
