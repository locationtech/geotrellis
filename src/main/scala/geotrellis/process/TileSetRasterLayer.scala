package geotrellis.process

import geotrellis._
import geotrellis.raster._
import geotrellis.util._
import geotrellis.data.arg.ArgReader
import geotrellis.raster._

import com.typesafe.config.Config
import java.io.File

import scalaxy.loops._
import scala.collection.mutable

object TileSetRasterLayerBuilder
extends RasterLayerBuilder {
  def apply(jsonPath:String, json:Config):Option[RasterLayer] = {
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
      System.err.println(s"[ERROR] Raster in catalog points Tile Directory '${tileDir.getPath}'" +
                          ", but this is not a valid directory.")
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
                                 getYskew(json),
                                 layout,
                                 getCacheFlag(json))

      Some(new TileSetRasterLayer(info,tileDirPath,layout))
    }
  }
}

object TileSetRasterLayer {
  def tileCacheName(info:RasterLayerInfo,col:Int,row:Int) = 
    s"${info.name}_${col}_${row}"
}

class TileSetRasterLayer(info:RasterLayerInfo, 
                         val tileDirPath:String,
                         val tileLayout:TileLayout)
extends RasterLayer(info) {
  def getRaster(targetExtent:Option[RasterExtent]) = {
    targetExtent match {
      case Some(re) =>
        // If a specific raster extent is asked for,
        // load an ArrayRasterData for the extent.
        // TODO: Is this the best strategy? Or should
        // tile rasters loaded with different extents\resolutions
        // produce tiled rasters?

        // Create destination raster data
        val data = RasterData.emptyByType(info.rasterType,re.cols,re.rows)

        // Collect data from intersecting tiles
        val targetExtent = re.extent
        val resLayout = tileLayout.getResolutionLayout(info.rasterExtent)
        for(tcol <- 0 until tileLayout.tileCols optimized) { 
          for(trow <- 0 until tileLayout.tileRows optimized) {
            val sourceRasterExtent = resLayout.getRasterExtent(tcol,trow)
            val sourceExtent = resLayout.getExtent(tcol,trow)
            sourceExtent.intersect(targetExtent) match {
              case Some(ext) =>
                val cols = math.ceil((ext.xmax - ext.xmin) / re.cellwidth).toInt
                val rows = math.ceil((ext.ymax - ext.ymin) / re.cellheight).toInt
                val tileRe = RasterExtent(ext,re.cellwidth,re.cellheight,cols,rows)

                // Read section of the tile
                val path = Tiler.tilePath(tileDirPath, info.name, tcol, trow)
                val sourceRasterExtent = resLayout.getRasterExtent(tcol,trow)
                val rasterPart = 
                  new ArgReader(path).readPath(info.rasterType,sourceRasterExtent,tileRe)

                // Copy over the values to the correct place in the raster data
                for(partCol <- 0 until cols optimized) {
                  for(partRow <- 0 until rows optimized) {
                    val dataCol = re.mapXToGrid(tileRe.gridColToMap(partCol))
                    val dataRow = re.mapYToGrid(tileRe.gridRowToMap(partRow))
                    if(!(dataCol < 0 || dataCol >= re.cols ||
                         dataRow < 0 || dataRow >= re.rows)) {
                      if(info.rasterType.isDouble) {
                        data.setDouble(dataCol, dataRow, rasterPart.getDouble(partCol, partRow))
                      } else {
                        data.set(dataCol, dataRow, rasterPart.get(partCol, partRow))
                      }
                    }
                  }
                }

              case None => // pass
            }
          }
        }
        Raster(data, re)
      case None => 
        val loader = getTileLoader()
        val tiles = mutable.ListBuffer[Raster]()
        for(col <- 0 until tileLayout.tileCols optimized) {
          for(row <- 0 until tileLayout.tileRows optimized) {
            tiles += loader.getTile(col,row,None)
          }
        }
        TileRaster(tiles.toSeq, info.rasterExtent, tileLayout).toArrayRaster
    }
  }

  override
  def getRaster(extent:Extent):Raster = 
    CroppedRaster(getRaster(None),extent)

  def getTile(col:Int, row:Int, targetExtent:Option[RasterExtent]) = 
    getTileLoader().getTile(col,row,targetExtent)

  def getTileLoader() =
    if(isCached)
      new CacheTileLoader(info,tileLayout,getCache)
    else 
      new DiskTileLoader(info,tileLayout,tileDirPath)

  def cache(c:Cache) = {
    for(col <- 0 until tileLayout.tileCols) {
      for(row <- 0 until tileLayout.tileRows) {
        val path = Tiler.tilePath(tileDirPath, info.name, col, row)
        c.insert(TileSetRasterLayer.tileCacheName(info,col,row), Filesystem.slurp(path))
      }
    }
  }
}

abstract class TileLoader(tileSetInfo:RasterLayerInfo,
                          tileLayout:TileLayout) extends Serializable {
  val resLayout = tileLayout.getResolutionLayout(tileSetInfo.rasterExtent)

  val rasterExtent = tileSetInfo.rasterExtent

  def getTile(col:Int,row:Int,targetExtent:Option[RasterExtent]):Raster = {
    val re = resLayout.getRasterExtent(col,row)
    if(col < 0 || row < 0 ||
       tileLayout.tileCols <= col || tileLayout.tileRows <= row) {
      val tre = 
        targetExtent match {
          case Some(x) => x
          case None => re
        }

      Raster(IntConstant(NODATA, tre.cols, tre.rows),  rasterExtent)
    } else {
      loadRaster(col,row,re,targetExtent)
    }
  }

  protected def loadRaster(col:Int,row:Int,re:RasterExtent,tre:Option[RasterExtent]):Raster
}

class DiskTileLoader(tileSetInfo:RasterLayerInfo,
                     tileLayout:TileLayout,
                     tileDirPath:String)
extends TileLoader(tileSetInfo,tileLayout) {
  def loadRaster(col:Int,row:Int,re:RasterExtent,targetExtent:Option[RasterExtent]) = {
      val path = Tiler.tilePath(tileDirPath, tileSetInfo.name, col, row)
    val reader = new ArgReader(path)
    val tre =
      targetExtent match {
        case Some(x) => x
        case None => re
      }
      reader.readPath(tileSetInfo.rasterType,re,tre)
  }
}

class CacheTileLoader(tileSetInfo:RasterLayerInfo,
                      tileLayout:TileLayout,
                      c:Cache)
extends TileLoader(tileSetInfo,tileLayout) {
  def loadRaster(col:Int,row:Int,re:RasterExtent,targetExtent:Option[RasterExtent]) = {
    c.lookup[Array[Byte]](TileSetRasterLayer.tileCacheName(tileSetInfo,col,row)) match {
      case Some(bytes) =>
        val reader = new ArgReader("")
        val tre = 
          targetExtent match {
            case Some(x) => x
            case None => re
          }
        reader.readCache(bytes, tileSetInfo.rasterType, re, tre)
      case None =>
        sys.error("Cache problem: Tile thinks it's cached but it is in fact not cached.")
    }
  }
}
