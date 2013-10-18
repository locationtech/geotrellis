package geotrellis

import geotrellis.raster._
import scalaxy.loops._
import scala.collection.mutable

object CroppedRaster {
  def apply(sourceRaster:Raster,gridBounds:GridBounds):CroppedRaster =
    CroppedRaster(sourceRaster,gridBounds,sourceRaster.rasterExtent.extentFor(gridBounds))
  def apply(sourceRaster:Raster,extent:Extent):CroppedRaster =
    CroppedRaster(sourceRaster,sourceRaster.rasterExtent.gridBoundsFor(extent),extent)
}

case class CroppedRaster(sourceRaster:Raster,
                         gridBounds:GridBounds,
                         extent:Extent) 
  extends Raster {
  val rasterExtent = RasterExtent(extent,
                                  sourceRaster.rasterExtent.cellwidth,
                                  sourceRaster.rasterExtent.cellheight,
                                  gridBounds.width,
                                  gridBounds.height)
  def force = toArrayRaster
  val rasterType = sourceRaster.rasterType

  def get(col: Int,row: Int): Int = 
    sourceRaster.get(col+gridBounds.colMin,row+gridBounds.rowMin)
  def getDouble(col: Int,row: Int): Double = 
    sourceRaster.getDouble(col+gridBounds.colMin,row+gridBounds.rowMin)

  def toArray: Array[Int] = {
    val arr = Array.ofDim[Int](rasterExtent.cols*rasterExtent.rows)
    val colMin = gridBounds.colMin
    val colMax = gridBounds.colMax
    val rowMin = gridBounds.rowMin
    val rowMax = gridBounds.rowMax
    var i = 0
    for(row <-rowMin until rowMax optimized) {
      for(col <- rowMax until colMax optimized) {
        arr(i) = sourceRaster.get(col,row)
        i += 1
      }
    }
    arr
  }

  def toArrayRaster:ArrayRaster = {
    val data = RasterData.allocByType(rasterType,cols,rows)
    val colMin = gridBounds.colMin
    val colMax = gridBounds.colMax
    val rowMin = gridBounds.rowMin
    val rowMax = gridBounds.rowMax
    if(!isFloat) {
      for(row <-rowMin until rowMax optimized) {
        for(col <- rowMax until colMax optimized) {
          data.set(col-colMin, row-rowMin, sourceRaster.get(col,row))
        }
      }
    } else {
      for(row <-rowMin until rowMax optimized) {
        for(col <- rowMax until colMax optimized) {
          data.setDouble(col-colMin, row-rowMin, sourceRaster.getDouble(col,row))
        }
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def toArrayDouble: Array[Double] = {
    val arr = Array.ofDim[Double](rasterExtent.cols*rasterExtent.rows)
    val colMin = gridBounds.colMin
    val colMax = gridBounds.colMax
    val rowMin = gridBounds.rowMin
    val rowMax = gridBounds.rowMax
    var i = 0
    for(row <-rowMin until rowMax optimized) {
      for(col <- rowMax until colMax optimized) {
        arr(i) = sourceRaster.getDouble(col,row)
        i += 1
      }
    }
    arr
  }

  def copy() = 
    if(isFloat) {
      Raster(toArray,rasterExtent) 
    } else {
      Raster(toArrayDouble,rasterExtent)
    }

  def convert(typ: RasterType):Raster = 
    sourceRaster.convert(typ)

  def map(f: Int => Int): Raster = {
    val data = RasterData.allocByType(rasterType,cols,rows)
    val colMin = gridBounds.colMin
    val colMax = gridBounds.colMax
    val rowMin = gridBounds.rowMin
    val rowMax = gridBounds.rowMax
    var i = 0
    for(row <-rowMin until rowMax optimized) {
      for(col <- rowMax until colMax optimized) {
        data.set(col,row, sourceRaster.get(col,row))
        i += 1
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def combine(r2:Raster)(f:(Int, Int) => Int):Raster = {
    if(this.rasterExtent != r2.rasterExtent) {
      throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
                             s"$rasterExtent does not match ${r2.rasterExtent}")
    }
    r2 match {
      case ar:ArrayRaster =>
        val data = RasterData.allocByType(rasterType,cols,rows)
        val colMin = gridBounds.colMin
        val colMax = gridBounds.colMax
        val rowMin = gridBounds.rowMin
        val rowMax = gridBounds.rowMax
        for(row <-rowMin until rowMax optimized) {
          for(col <- rowMax until colMax optimized) {
            data.set(col,row, f(sourceRaster.get(col,row),ar.get(col-colMin,row-rowMin)))
          }
        }
        Raster(data,rasterExtent)
      case _ => sys.error("Unknown Raster type")
      // case tr:TileRaster =>
      //   TileRaster.wrap(toArrayRaster,tr.tileLayout).combine(tr)(f)
    }
  }

  def mapDouble(f:Double =>Double):Raster = {
    val data = RasterData.allocByType(rasterType,cols,rows)
    val colMin = gridBounds.colMin
    val colMax = gridBounds.colMax
    val rowMin = gridBounds.rowMin
    val rowMax = gridBounds.rowMax
    var i = 0
    for(row <-rowMin until rowMax optimized) {
      for(col <- rowMax until colMax optimized) {
        data.setDouble(col,row, sourceRaster.getDouble(col,row))
        i += 1
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def combineDouble(r2:Raster)(f:(Double, Double) => Double):Raster = {
    if(this.rasterExtent != r2.rasterExtent) {
      throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
                             s"$rasterExtent does not match ${r2.rasterExtent}")
    }
    r2 match {
      case ar:ArrayRaster =>
        val data = RasterData.allocByType(rasterType,cols,rows)
        val colMin = gridBounds.colMin
        val colMax = gridBounds.colMax
        val rowMin = gridBounds.rowMin
        val rowMax = gridBounds.rowMax
        for(row <-rowMin until rowMax optimized) {
          for(col <- rowMax until colMax optimized) {
            data.setDouble(col,row, f(sourceRaster.get(col,row),ar.getDouble(col-colMin,row-rowMin)))
          }
        }
        Raster(data,rasterExtent)
      case _ => sys.error("Unknown Raster type")
      // case tr:TileRaster =>
      //   TileRaster.wrap(toArrayRaster,tr.tileLayout).combineDouble(tr)(f)
    }
  }
}
