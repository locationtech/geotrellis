package trellis.benchmark

import trellis._

import scala.util.Random

import Predef.{any2stringadd => _, _}
import scala.{specialized => spec}
import com.azavea.math.{Numeric => Num}
import com.azavea.math.FastImplicits._

object DataMapBenchmark {

  val warmup = 50
  val times = 400
  val h = 1024
  val w = 1024

  val n = h * w

  def array(data:Array[Int]) = {
    val data2 = data.clone
    var i = 0
    val len = data2.length
    while (i < len) {
      data2(i) = data(i) * 2
      i += 1
    }
    data2
  }

  def direct(raster:IntRaster) = {
    val data = raster.data
    val raster2 = raster.copy
    val data2 = raster2.data
    var i = 0
    val len = data2.length
    while (i < len) {
      data2(i) = data(i) * 2
      i += 1
    }

    raster2
  }

  def indirect(raster:IntRaster) = raster.map(z => z * 2)

  def generic(raster:GenRaster[Int]) = raster.map(z => z * 2)

  def moreGeneric[@spec T:Num](raster:GenRaster[T]) = raster.map(z => z * numeric.fromInt(2))

  def main(args:Array[String]) {
    var arrayTime = 0L
    var directTime = 0L
    var indirectTime = 0L
    var genericTime = 0L
    var moreGenericTime = 0L

    val data = Array.ofDim[Int](n).map(i => Random.nextInt())
    val extent = Extent(0, 0, w, h)
    val re = RasterExtent(extent, 1.0, 1.0, w, h)

    val raster = IntRaster(data, re)

    val graster = GenRaster[Int](data, re)

    var currGenRaster = graster
    var currRaster = raster
    var currData = data

    def runit() {
      val t0 = System.currentTimeMillis()
      currData = array(data)
      arrayTime += System.currentTimeMillis() - t0

      val t1 = System.currentTimeMillis()
      currRaster = direct(raster)
      directTime += System.currentTimeMillis() - t1

      val t2 = System.currentTimeMillis()
      currRaster = indirect(raster)
      indirectTime += System.currentTimeMillis() - t2

      val t3 = System.currentTimeMillis()
      currGenRaster = generic(graster)
      genericTime += System.currentTimeMillis() - t3

      val t4 = System.currentTimeMillis()
      currGenRaster = moreGeneric(graster)
      moreGenericTime += System.currentTimeMillis() - t4
    }

    println("doing %d warmup iterations" format warmup)
    for (i <- 0 until warmup) {
      runit()
      System.gc()
    }

    arrayTime = 0L
    directTime = 0L
    indirectTime = 0L
    genericTime = 0L
    moreGenericTime = 0L

    println("running each test %d times" format times)
    for (i <- 0 until times) {
      runit()
    }

    output("array", arrayTime)
    output("direct", directTime)
    output("indirect", indirectTime)
    output("generic", genericTime)
    output("more-generic", moreGenericTime)
  }

  def output(label:String, t:Long) {
    val p = (t * 1.0) / times
    println("%-10s took %4d ms (%.3f ms/per)" format (label, t, p))
  }
}
