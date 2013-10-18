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
      case ar:ArrayRaster =>
        wrap(ar, tileLayout)
      case _ =>
        sys.error(s"TileRaster cannot handle this raster type (${r.getClass.getSimpleName})")
    }

  def wrap(ar:ArrayRaster,tileLayout:TileLayout):TileRaster = {
    TileRaster(split(ar,tileLayout),ar.rasterExtent,tileLayout)
  }

  def split(ar:Raster,tileLayout:TileLayout):Seq[Raster] = {
    val pCols = tileLayout.pixelCols
    val pRows = tileLayout.pixelRows

    val tiles = mutable.ListBuffer[Raster]()
    for(trow <- 0 until tileLayout.tileRows optimized) {
      for(tcol <- 0 until tileLayout.tileCols optimized) {
        val firstCol = tcol * pCols
        val lastCol = tcol + pCols - 1
        val firstRow = tcol * pRows
        val lastRow = tcol + pRows - 1
        tiles += CroppedRaster(ar,GridBounds(firstCol,firstRow,lastCol,lastRow))
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
      if(!isFloat) {
        for(tcol <- 0 until tileLayout.tileCols optimized) {
          for(trow <- 0 until tileLayout.tileRows optimized) {
            val tile = getTile(tcol,trow)
            for(prow <- 0 until tileLayout.pixelRows) {
              for(pcol <- 0 until tileLayout.pixelCols) {
                val acol = (tileLayout.pixelCols * tcol) + pcol
                val arow = (tileLayout.pixelRows * trow) + prow
                data.set(acol,arow,tile.get(pcol,prow))
              }
            }
          }
        }
      } else {
        for(tcol <- 0 until tileLayout.tileCols optimized) {
          for(trow <- 0 until tileLayout.tileRows optimized) {
            val tile = getTile(tcol,trow)
            for(prow <- 0 until tileLayout.pixelRows) {
              for(pcol <- 0 until tileLayout.pixelCols) {
                val acol = (tileLayout.pixelCols * tcol) + pcol
                val arow = (tileLayout.pixelRows * trow) + prow
                data.setDouble(acol,arow,tile.getDouble(pcol,prow))
              }
            }
          }
        }
      }
      ArrayRaster(data,rasterExtent)
    }
  }

  // def toArray:Array[Int] = {
  //   val cols = tileLayout.totalCols
  //   val rows = tileLayout.totalRows
  //   if (cols.toLong*rows.toLong > 2147483647L) {
  //     sys.error("This tiled raster is too big to convert into an array.") 
  //   } else {
  //     val len = cols*rows
  //     val d = Array.ofDim[Int](tileLayout.totalCols*tileLayout.totalRows)
  //     for(tcol <- 0 until tileLayout.tileCols optimized) {
  //       for(trow <- 0 until tileLayout.tileRows optimized) {
  //         val tile = getTile(tcol,trow)
  //         for(prow <- 0 until tileLayout.pixelRows) {
  //           for(pcol <- 0 until tileLayout.pixelCols) {
  //             val acol = (tileLayout.pixelCols * tcol) + pcol
  //             val arow = (tileLayout.pixelRows * trow) + prow
  //             d(arow*cols+acol) = tile.get(pcol,prow)
  //           }
  //         }
  //       }
  //     }
  //     d
  //   }
  // }

  // def toArrayDouble:Array[Double] = {
  //   val cols = tileLayout.totalCols
  //   val rows = tileLayout.totalRows
  //   if (cols.toLong*rows.toLong > 2147483647L) {
  //     sys.error("This tiled raster is too big to convert into an array.") 
  //   } else {
  //     val len = cols*rows
  //     val d = Array.ofDim[Double](tileLayout.totalCols*tileLayout.totalRows)
  //     for(tcol <- 0 until tileLayout.tileCols optimized) {
  //       for(trow <- 0 until tileLayout.tileRows optimized) {
  //         val tile = getTile(tcol,trow)
  //         for(prow <- 0 until tileLayout.pixelRows) {
  //           for(pcol <- 0 until tileLayout.pixelCols) {
  //             val acol = (tileLayout.pixelCols * tcol) + pcol
  //             val arow = (tileLayout.pixelRows * trow) + prow
  //             d(arow*cols+acol) = tile.getDouble(pcol,prow)
  //           }
  //         }
  //       }
  //     }
  //     d
  //   }
  // }

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

  //def copy():Raster = TileRaster(tileList.map(_.copy),rasterExtent,tileLayout)
  //def convert(typ:RasterType) = TileRaster(tileList.map(_.convert(typ)),rasterExtent,tileLayout)

  // def foreach(f:Int => Unit):Unit = 
  //   tileList.foreach(_.foreach(f))
  // def map(f:Int=>Int):Raster = 
  //   TileRaster(tileList.map(_.map(f)),rasterExtent,tileLayout)
  // def combine(r2:Raster)(f:(Int, Int) => Int):Raster = {
  //   if(this.rasterExtent != r2.rasterExtent) {
  //     throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
  //                            s"$rasterExtent does not match ${r2.rasterExtent}")
  //   }
  //   r2 match {
  //     case ar:ArrayRaster => 
  //       combine(TileRaster.wrap(ar,tileLayout))(f)
  //     case tr:TileRaster =>
  //       if(this.tileLayout != tr.tileLayout) {
  //         throw new GeoAttrsError("Cannot combine tile rasters with different tile layouts." +  
  //                                s"$tileLayout does not match ${tr.tileLayout}")
  //       }
  //       val combinedTiles = 
  //         this.tileList.zip(tr.tileList).map { case (t1,t2) => t1.combine(t2)(f) }
  //       TileRaster(combinedTiles,rasterExtent,tileLayout)
  //   }
  // }
  // def combine(rs:Seq[Raster])(f:Seq[Int] => Int):Raster = {
  //   if(rs.isEmpty) { return this }
  //   val allTiles:List[List[Raster]] = collectTiles(rs)

  //   val combinedTiles = 
  //     allTiles.map { tiles =>
  //       val data = RasterData.allocByType(rasterType,tileLayout.pixelCols,tileLayout.pixelRows)
  //       for(row <- 0 until tileLayout.pixelRows) {
  //         for(col <- 0 until tileLayout.pixelCols) {
  //           data.set(col,row, f(tiles.map(_.get(col,row))))
  //         }
  //       }
  //       ArrayRaster(data,tiles.head.rasterExtent)
  //     }
  //   TileRaster(combinedTiles,rasterExtent,tileLayout)
  // }

  // def foreachDouble(f:Double => Unit):Unit = 
  //   tileList.foreach(_.foreachDouble(f))
  // def mapDouble(f:Double => Double):Raster = 
  //   TileRaster(tileList.map(_.mapDouble(f)),rasterExtent,tileLayout)
  // def combineDouble(r2:Raster)(f:(Double, Double) => Double):Raster = {
  //   if(this.rasterExtent != r2.rasterExtent) {
  //     throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
  //                            s"$rasterExtent does not match ${r2.rasterExtent}")
  //   }
  //   r2 match {
  //     case ar:ArrayRaster => 
  //       combineDouble(TileRaster.wrap(ar,tileLayout))(f)
  //     case tr:TileRaster =>
  //       if(this.tileLayout != tr.tileLayout) {
  //         throw new GeoAttrsError("Cannot combine tile rasters with different tile layouts." +  
  //                                s"$tileLayout does not match ${tr.tileLayout}")
  //       }
  //       val combinedTiles = 
  //         this.tileList.zip(tr.tileList).map { case (t1,t2) => t1.combineDouble(t2)(f) }
  //       TileRaster(combinedTiles,rasterExtent,tileLayout)
  //   }
  // }
  // def combineDouble(rs:Seq[Raster])(f:Seq[Double] => Double):Raster = {
  //   if(rs.isEmpty) { return this }
  //   val allTiles:List[List[Raster]] = collectTiles(rs)

  //   val combinedTiles = 
  //     allTiles.map { tiles =>
  //       val data = RasterData.allocByType(rasterType,tileLayout.pixelCols,tileLayout.pixelRows)
  //       for(row <- 0 until tileLayout.pixelRows) {
  //         for(col <- 0 until tileLayout.pixelCols) {
  //           data.setDouble(col,row, f(tiles.map(_.getDouble(col,row))))
  //         }
  //       }
  //       ArrayRaster(data,tiles.head.rasterExtent)
  //     }
  //   TileRaster(combinedTiles,rasterExtent,tileLayout)
  // }

  // /** Function to collect a list of list of tiles, converting any non-tiled
  //  *  rasters into tiled rasters, for the purpose of combine and combineDouble
  //  */
  // private def collectTiles(rs:Seq[Raster]) = {
  //   val tileRasters:List[List[Raster]] = 
  //     rs.map{ r =>
  //              if(this.rasterExtent != r.rasterExtent) {
  //                throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
  //                  s"$rasterExtent does not match ${r.rasterExtent}")
  //              }
  //              r match {
  //                case ar:ArrayRaster =>
  //                  TileRaster.wrap(ar,tileLayout).tileList
  //                case tr:TileRaster =>
  //                  if(this.tileLayout != tr.tileLayout) {
  //                    throw new GeoAttrsError("Cannot combine tile rasters with different tile layouts." +
  //                      s"$tileLayout does not match ${tr.tileLayout}")
  //                  }
  //                  tr.tileList
  //              }
  //           }
  //       .toList
  //   tileRasters.foldLeft(tileList.map(List(_)))(_.zip(_).map { case(l,r) => l :+ r })
  // }

}
