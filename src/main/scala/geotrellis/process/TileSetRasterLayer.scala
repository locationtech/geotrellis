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
  def apply(ds:Option[String],jsonPath:String, json:Config):RasterLayer = {
    val tileDir = 
      if(json.hasPath("path")) {
        val f = new File(json.getString("path"))
        if(f.isAbsolute) {
          f
        } else {
          new File(new File(jsonPath).getParent, f.getPath)
        }
      } else {
        // Default to a directory with the same name as the layer name.
        new File(new File(jsonPath).getParent, getName(json))
      }

    if(!tileDir.isDirectory) {
      throw new java.io.IOException(s"[ERROR] Raster in catalog points Tile Directory '${tileDir.getPath}'" +
                                     ", but this is not a valid directory.")
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

      val info = 
        RasterLayerInfo(
          LayerId(ds,getName(json)),
          getRasterType(json),
          rasterExtent,
          getEpsg(json),
          getXskew(json),
          getYskew(json),
          layout,
          getCacheFlag(json)
        )

      new TileSetRasterLayer(info,tileDirPath,layout)
    }
  }
}

object TileSetRasterLayer {
  def tileName(id:LayerId,col:Int,row:Int) = 
    s"${id}_${col}_${row}"

  def tilePath(path:String, id:LayerId, col:Int, row:Int) =
    Filesystem.join(path, s"${id.name}_${col}_${row}.arg")
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

        // Create destination raster data
        val data = RasterData.emptyByType(info.rasterType,re.cols,re.rows)

        // Collect data from intersecting tiles
        val targetExtent = re.extent
        val resLayout = tileLayout.getResolutionLayout(info.rasterExtent)
        val loader = getTileLoader()
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
                val rasterPart = loader.getTile(tcol,trow,Some(tileRe))

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

  def cache(c:Cache[String]) = {
    for(col <- 0 until tileLayout.tileCols) {
      for(row <- 0 until tileLayout.tileRows) {
        val path = TileSetRasterLayer.tilePath(tileDirPath, info.id, col, row)
        c.insert(TileSetRasterLayer.tileName(info.id,col,row), Filesystem.slurp(path))
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
    val path = TileSetRasterLayer.tilePath(tileDirPath, tileSetInfo.id, col, row)
    targetExtent match {
      case Some(tre) => 
        ArgReader.read(path,tileSetInfo.rasterType,re,tre)
      case None => 
        ArgReader.read(path,tileSetInfo.rasterType,re)
    }

  }
}

class CacheTileLoader(info:RasterLayerInfo,
                      tileLayout:TileLayout,
                      c:Cache[String])
extends TileLoader(info,tileLayout) {
  def loadRaster(col:Int,row:Int,re:RasterExtent,targetExtent:Option[RasterExtent]) = {
    c.lookup[Array[Byte]](TileSetRasterLayer.tileName(info.id,col,row)) match {
      case Some(bytes) =>
        targetExtent match {
          case Some(tre) => 
            val data = ArgReader.warpBytes(bytes,info.rasterType,re,tre)
            Raster(data,tre)
          case None => 
            val data = RasterData.fromArrayByte(bytes,info.rasterType,re.cols,re.rows)
            Raster(data,re)
        }
      case None =>
        sys.error("Cache problem: Tile thinks it's cached but it is in fact not cached.")
    }
  }
}
