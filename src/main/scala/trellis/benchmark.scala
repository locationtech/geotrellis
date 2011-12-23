package trellis.benchmark

import trellis.raster.IntRaster
import trellis.{Extent,RasterExtent}

import trellis.constant.NODATA

import scala.util.Random

object Main {

  val warmup = 50
  val times = 400
  val h = 1024
  val w = 1024

  val n = h * w

  val data = Array.ofDim[Int](n).map(i => Random.nextInt())

  val extent = Extent(0, 0, w, h)
  val geo = RasterExtent(extent, 1.0, 1.0, w, h)
  val raster = IntRaster(data, h, w, geo)

  def direct1(raster:IntRaster) = {
    val raster2 = raster.copy
     val data2 = raster2.data

    var i = 0
    val len = data.length
    while (i < len) {
      data2(i) = data(i) * 2
      i += 1
    }

    raster2
  }

  def direct2(raster:IntRaster) = {
    val raster2 = raster.copy
     val data2 = raster2.data

    var i = 0
    val len = data.length
    while (i < len) {
      data2(i) = data(i) + 16
      i += 1
    }

    raster2
  }

  def indirect1(raster:IntRaster) = raster.map(z => z * 2)
  def indirect2(raster:IntRaster) = raster.map(z => z + 16)

  def main(args:Array[String]) {
    var directTime = 0L
    var indirectTime = 0L

    var currRaster = raster

    def runit() {
      val t0 = System.currentTimeMillis()
      currRaster = direct1(currRaster)
      directTime += System.currentTimeMillis() - t0

      val t1 = System.currentTimeMillis()
      currRaster = indirect1(currRaster)
      indirectTime += System.currentTimeMillis() - t1

      val t2 = System.currentTimeMillis()
      currRaster = direct2(currRaster)
      directTime += System.currentTimeMillis() - t2

      val t3 = System.currentTimeMillis()
      currRaster = indirect2(currRaster)
      indirectTime += System.currentTimeMillis() - t3
    }

    for (i <- 0 until warmup) {
      currRaster = raster
      runit()
      System.gc()
    }

    directTime = 0L
    indirectTime = 0L

    for (i <- 0 until times) {
      currRaster = raster
      runit()
    }

    println("  direct took %d ms".format(directTime))
    println("indirect took %d ms".format(indirectTime))
  }
}
