package geotrellis

import geotrellis.raster._
import scalaxy.loops._
import scala.collection.mutable

object TileRaster {
  def apply(r:Raster,tileLayout:TileLayout):TileRaster =
    r match {
      case tr:TileRaster =>
        if(tileLayout != tr.tileLayout) {
          throw new GeoAttrsError("This raster is a tile raster with a different layout than" +
                                  " the argument tile layout." +  
                                 s" $tileLayout does not match ${tr.tileLayout}")
        }
        tr
      case _ =>
        wrap(r, tileLayout)
//        sys.error(s"TileRaster cannot handle this raster type (${r.getClass.getSimpleName})")
    }

  def wrap(r:Raster,tileLayout:TileLayout):TileRaster = {
    TileRaster(split(r,tileLayout),r.rasterExtent,tileLayout)
  }

  def split(r:Raster,tileLayout:TileLayout):Seq[Raster] = {
    val pCols = tileLayout.pixelCols
    val pRows = tileLayout.pixelRows

    val tiles = mutable.ListBuffer[Raster]()
    for(trow <- 0 until tileLayout.tileRows optimized) {
      for(tcol <- 0 until tileLayout.tileCols optimized) {
        val firstCol = tcol * pCols
        val lastCol = firstCol + pCols - 1
        val firstRow = trow * pRows
        val lastRow = firstRow + pRows - 1
        val gb = GridBounds(firstCol,firstRow,lastCol,lastRow)
        tiles += CroppedRaster(r,gb)
      }
    }
    return tiles
  }
}

case class TileRaster(tiles:Seq[Raster],
                      rasterExtent:RasterExtent,
                      tileLayout:TileLayout) extends RasterLike {
  private val tileList = tiles.toList
  private val tileCols = tileLayout.tileCols
  private def getTile(tcol:Int,trow:Int) = tileList(trow*tileCols+tcol)

  val rasterType = tiles(0).rasterType

  def toArrayRaster:ArrayRaster = {
    val data = RasterData.allocByType(rasterType,cols,rows)
    if (cols.toLong*rows.toLong > 2147483647L) {
      sys.error("This tiled raster is too big to convert into an array.") 
    } else {
      val len = cols*rows
      val tileCols = tileLayout.tileCols
      val tileRows = tileLayout.tileRows
      val pixelCols = tileLayout.pixelCols
      val pixelRows = tileLayout.pixelRows
      if(!isFloat) {
        for(tcol <- 0 until tileCols optimized) {
          for(trow <- 0 until tileRows optimized) {
            val tile = getTile(tcol,trow)
            for(prow <- 0 until pixelRows optimized) {
              for(pcol <- 0 until pixelCols optimized) {
                val acol = (pixelCols * tcol) + pcol
                val arow = (pixelRows * trow) + prow
                data.set(acol,arow,tile.get(pcol,prow))
              }
            }
          }
        }
      } else {
        for(tcol <- 0 until tileCols optimized) {
          for(trow <- 0 until tileRows optimized) {
            val tile = getTile(tcol,trow)
            for(prow <- 0 until pixelRows optimized) {
              for(pcol <- 0 until pixelCols optimized) {
                val acol = (pixelCols * tcol) + pcol
                val arow = (pixelRows * trow) + prow
                data.setDouble(acol,arow,tile.getDouble(pcol,prow))
              }
            }
          }
        }
      }
      ArrayRaster(data,rasterExtent)
    }
  }

  def get(col:Int, row:Int):Int = {
    val tcol = col / tileLayout.pixelCols
    val trow = row / tileLayout.pixelRows
    val pcol = col % tileLayout.pixelCols
    val prow = row % tileLayout.pixelRows

    getTile(tcol, trow).get(pcol, prow)
  }

  def getDouble(col:Int, row:Int) = {
    val tcol = col / tileLayout.pixelCols
    val trow = row / tileLayout.pixelRows
    val pcol = col % tileLayout.pixelCols
    val prow = row % tileLayout.pixelRows
    getTile(tcol, trow).getDouble(pcol, prow)
  }

  override
  def asciiDraw():String = {
    val sb = new StringBuilder
    for(tileRow <- 0 until tileLayout.tileRows) {
      for(row <- 0 until tileLayout.pixelRows) {
        for(tileCol <- 0 until tileLayout.tileCols) {
          val tile = getTile(tileCol,tileRow)

          for(col <- 0 until tileLayout.pixelCols) {
            val v = tile.get(col,row)
            val s = if(v == NODATA) {
              "ND"
            } else {
              s"$v"
            }
            val pad = " " * math.max(6 - s.length,0)
            sb.append(s"$pad$s")
          }
          if(tileCol != tileLayout.tileCols - 1) {
            val pad = " " * 5
            sb.append(s"$pad| ")
          }
        }
        sb.append(s"\n")
      }
      if(tileRow != tileLayout.tileRows - 1) {
        val rowDiv = "-" * (6 * tileLayout.pixelCols * tileLayout.tileCols - 2) + 
                     "-" * (6 * tileLayout.tileCols)
        sb.append(s"  $rowDiv\n")
      }
    }
    sb.toString
  }
}
