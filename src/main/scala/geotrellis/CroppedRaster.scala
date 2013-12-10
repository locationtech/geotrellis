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

  private val colMin = gridBounds.colMin
  private val rowMin = gridBounds.rowMin
  private val sourceCols = sourceRaster.rasterExtent.cols
  private val sourceRows = sourceRaster.rasterExtent.rows

  def get(col: Int,row: Int): Int = {
    val c = col+gridBounds.colMin
    val r = row+gridBounds.rowMin
    if(c < 0 || r < 0 || c >= sourceCols || r >= sourceRows) {
      NODATA
    } else {
      sourceRaster.get(c,r)
    }
  }
  def getDouble(col: Int,row: Int): Double = {
    val c = col+gridBounds.colMin
    val r = row+gridBounds.rowMin

    if(c < 0 || r < 0 || c >= sourceCols || r >= sourceRows) {
      Double.NaN
    } else {
      sourceRaster.getDouble(col+gridBounds.colMin,row+gridBounds.rowMin)
    }
  }

  def toArrayRaster:ArrayRaster = {
    val data = RasterData.allocByType(rasterType,cols,rows)
    if(!isFloat) {
      for(row <- 0 until rows optimized) {
        for(col <- 0 until cols optimized) {
          data.set(col, row, get(col,row))
        }
      }
    } else {
      for(row <- 0 until rows optimized) {
        for(col <- 0 until cols optimized) {
          data.setDouble(col, row, getDouble(col,row))
        }
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def toArray: Array[Int] = {
    val arr = Array.ofDim[Int](rasterExtent.cols*rasterExtent.rows)
    var i = 0
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        arr(i) = get(col,row)
        i += 1
      }
    }
    arr
  }

  def toArrayDouble: Array[Double] = {
    val arr = Array.ofDim[Double](rasterExtent.cols*rasterExtent.rows)
    var i = 0
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        arr(i) = getDouble(col,row)
        i += 1
      }
    }
    arr
  }

  def toArrayByte(): Array[Byte] = toArrayRaster.toArrayByte

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
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        data.set(col,row, get(col,row))
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def combine(r2:Raster)(f:(Int, Int) => Int):Raster = {
    if(this.rasterExtent != r2.rasterExtent) {
      throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
                             s"$rasterExtent does not match ${r2.rasterExtent}")
    }
    val data = RasterData.allocByType(rasterType,cols,rows)
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        data.set(col,row, f(get(col,row),r2.get(col,row)))
      }
    }
    Raster(data,rasterExtent)
  }

  def mapDouble(f:Double =>Double):Raster = {
    val data = RasterData.allocByType(rasterType,cols,rows)
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        data.setDouble(col,row, getDouble(col,row))
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def combineDouble(r2:Raster)(f:(Double, Double) => Double):Raster = {
    if(this.rasterExtent != r2.rasterExtent) {
      throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
                             s"$rasterExtent does not match ${r2.rasterExtent}")
    }
    val data = RasterData.allocByType(rasterType,cols,rows)
    for(row <- 0 until rows optimized) {
      for(col <- 0 until cols optimized) {
        data.setDouble(col,row, f(getDouble(col,row),r2.getDouble(col,row)))
      }
    }
    Raster(data,rasterExtent)
  }
}
