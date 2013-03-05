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

  def createRaster(arr:Array[Double]) = {
    val d = scala.math.sqrt(arr.length).toInt
    if(d > scala.math.round(d)) { sys.error("Array must be square") }
    
    Raster(arr, RasterExtent(Extent(0,0,d,d),1,1,d,d))
  }

  def createRaster(arr:Array[Int],cols:Int,rows:Int) = {
    Raster(arr, RasterExtent(Extent(0,-rows,cols*10,0),10,1,cols,rows))
  }

  def createRaster(arr:Array[Double],cols:Int,rows:Int) = {
    Raster(arr, RasterExtent(Extent(0,-rows,cols*10,0),10,1,cols,rows))
  }

  def createNoData(cols:Int,rows:Int,t:RasterType = TypeInt) =
    Raster(RasterData.emptyByType(t,cols,rows), RasterExtent(Extent(0,-rows,cols*10,0),10,1,cols,rows))

  /**
   * 9x10 raster of 90 numbers between 1 - 100 in random order.
   */
  def positiveIntegerRaster = {
    val arr = Array(54, 62, 44, 75, 21, 56, 13,  5, 41,
                    66, 72, 63, 18, 28, 35, 45, 34, 46,
                    38, 36, 74, 77,  4, 71, 64, 93, 32,
                    81,  6, 80, 89,  7, 43, 37, 55,  3,
                    42, 15, 40, 31, 73, 70, 68, 78, 91,
                    98, 94, 79, 84,  8, 69, 96, 92, 85,
                    76, 86, 90, 59, 83,  9, 19, 23, 22,
                    33, 47, 29, 1, 39, 67, 49, 100, 25,
                    20, 53, 65, 17, 61, 50, 87, 99, 52,
                    11, 82, 30, 26, 27, 95, 97, 57, 14 )
    val ext = Extent(0,-100,900,0)
    val re = RasterExtent(ext, 100,10,9,10)
    Raster(arr,re)
  }

  /**
   * 9x10 TypeDouble raster with values between 0 and 1, exclusive.
   */
  def probabilityRaster = {
    val arr = Array(0.69, 0.06, 0.72, 0.45, 0.64, 0.17, 0.32, 0.07, 0.04,
                    0.65, 0.24, 0.26, 0.50, 0.34, 0.80, 0.05, 0.66, 0.91,
                    0.52, 0.92, 0.58, 0.46, 0.11, 0.57, 0.30, 0.71, 0.90,
                    0.59, 0.23, 0.60, 0.43, 0.70, 0.18, 0.86, 0.56, 0.84,
                    0.61, 0.39, 0.94, 0.51, 0.14, 0.67, 0.99, 0.89, 0.73,
                    0.85, 0.37, 0.31, 0.95, 0.47, 0.36, 0.97, 0.35, 0.25,
                    0.08, 0.50, 0.96, 0.38, 0.40, 0.22, 0.20, 0.63, 0.13,
                    0.09, 0.41, 0.02, 0.29, 0.54, 0.03, 0.62, 0.19, 0.53,
                    0.98, 0.82, 0.93, 0.27, 0.42, 0.44, 0.55, 0.15, 0.01,
                    0.74, 0.77, 0.75, 0.49, 0.33, 0.68, 0.79, 0.16, 0.78)
    val ext = Extent(0,-100,900,0)
    val re = RasterExtent(ext, 100,10,9,10)
    Raster(arr,re)
  }

  /**
   * 9x10 raster of 90 numbers between 1 - 100 in random order,
   * with NoData values in every even column.
   */
  def positiveIntegerNoDataRaster = {
    val n = NODATA
    val arr = Array(54,  n, 44,  n, 21,  n, 13,  n, 41,
                    66,  n, 63,  n, 28,  n, 45,  n, 46,
                    38,  n, 74,  n,  4,  n, 64,  n, 32,
                    81,  n, 80,  n,  7,  n, 37,  n,  3,
                    42,  n, 40,  n, 73,  n, 68,  n, 91,
                    98,  n, 79,  n,  8,  n, 96,  n, 85,
                    76,  n, 90,  n, 83,  n, 19,  n, 22,
                    33,  n, 29,  n, 39,  n, 49,  n, 25,
                    20,  n, 65,  n, 61,  n, 87,  n, 52,
                    11,  n, 30,  n, 27,  n, 97,  n, 14 )
    val ext = Extent(0,-100,900,0)
    val re = RasterExtent(ext, 100,10,9,10)
    Raster(arr,re)
  }

  /**
   * 9x10 TypeDouble raster with values between 0 and 1, exclusive,
   * with Double.NaN values in every even column.
   */
  def probabilityNoDataRaster = {
    val n = Double.NaN
    val arr = Array(0.69, n, 0.72, n, 0.64, n, 0.32, n, 0.04,
                    0.65, n, 0.26, n, 0.34, n, 0.05, n, 0.91,
                    0.52, n, 0.58, n, 0.11, n, 0.30, n, 0.90,
                    0.59, n, 0.60, n, 0.70, n, 0.86, n, 0.84,
                    0.61, n, 0.94, n, 0.14, n, 0.99, n, 0.73,
                    0.85, n, 0.31, n, 0.47, n, 0.97, n, 0.25,
                    0.08, n, 0.96, n, 0.40, n, 0.20, n, 0.13,
                    0.09, n, 0.02, n, 0.54, n, 0.62, n, 0.53,
                    0.98, n, 0.93, n, 0.42, n, 0.55, n, 0.01,
                    0.74, n, 0.75, n, 0.33, n, 0.79, n, 0.78)
    val ext = Extent(0,-100,900,0)
    val re = RasterExtent(ext, 100,10,9,10)
    Raster(arr,re)
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
