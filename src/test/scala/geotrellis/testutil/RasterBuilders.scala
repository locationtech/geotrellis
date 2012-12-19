package geotrellis.testutil

import geotrellis._
import geotrellis.process._

trait RasterBuilders {
  def createConsecutiveRaster(d:Int):Raster = {
    val arr = (for(i <- 1 to d*d) yield i).toArray
    Raster(arr, RasterExtent(Extent(0,0,d,d),1,1,d,d))
  }

  def createOnesRaster(d:Int):Raster = {
    val arr = (for(i <- 1 to d*d) yield 1).toArray
    Raster(arr, RasterExtent(Extent(0,0,d,d),1,1,d,d))
  }

  def createValueRaster(d:Int,v:Int):Raster = {
    val arr = (for(i <- 1 to d*d) yield v).toArray
    Raster(arr, RasterExtent(Extent(0,0,d,d),1,1,d,d))
  }

  def createRaster(arr:Array[Int]) = {
    val d = scala.math.sqrt(arr.length).toInt
    if(d > scala.math.round(d)) { sys.error("Array must be square") }
    Raster(arr, RasterExtent(Extent(0,0,d,d),1,1,d,d))
  }

  /* prints out a raster to console */
  def printR(r:Raster) {
    for(row <- 0 until r.rows) {
      for(col <- 0 until r.cols) {
        print(s"    ${r.get(col,row)}")
      }
      println
    }      
  }
}
