package geotrellis.benchmark

/*
 * # Caliper API key for jmarcus@azavea.com
 * postUrl: http://microbenchmarks.appspot.com:80/run/
 * apiKey: 3226081d-9776-40f4-a2d7-a1dc99c948c6
*/

import geotrellis._
import geotrellis.data._
import geotrellis.data.png._
import geotrellis.operation._
import geotrellis.process._
import geotrellis.raster._

import com.google.caliper.Benchmark
import com.google.caliper.Param
import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark

import scala.util.Random

/**
 * Extend this to create an actual benchmarking class.
 */
trait MyBenchmark extends SimpleBenchmark {

  /**
   * Sugar for building arrays using a per-cell init function.
   */
  def init[A:Manifest](size:Int)(init: => A) = {
    val data = Array.ofDim[A](size)
    for (i <- 0 until size) data(i) = init
    data
  }

  /**
   * Sugar to run 'f' for 'reps' number of times.
   */
  def run(reps:Int)(f: => Unit) = for(i <- 0 until reps)(f)
}

/**
 * Extend this to create a main object which will run 'cls' (a benchmark).
 */
abstract class MyRunner(cls:java.lang.Class[_ <: Benchmark]) {
  def main(args:Array[String]): Unit = Runner.main(cls, args:_*)
}

/**
 *
 */
object DataMap extends MyRunner(classOf[DataMap])
class DataMap extends MyBenchmark {
  @Param(Array("512/512", "512/1024", "1024/1024", "1024/2048", "2048/2048"))
  var dims:String = null

  var size:Int = 0
  var ints:Array[Int] = null
  var doubles:Array[Double] = null
  var raster:IntRaster = null
  var op:Op[IntRaster] = null

  var server:Server = null

  override def setUp() {
    val Array(h, w) = dims.split("/").map(_.toInt)
    size = h * w

    ints = init(size)(Random.nextInt)
    doubles = init(size)(Random.nextDouble)

    val re = RasterExtent(Extent(0, 0, w, h), 1.0, 1.0, w, h)
    raster = IntRaster(init(size)(Random.nextInt), re)

    op = MultiplyConstant(raster, 2)

    val catalogPath = "src/main/resources/catalog.json"
    val catalog = Catalog.fromPath(catalogPath)
    server = Server("demo", catalog)
  }

  def timeIntArray(reps:Int) = run(reps)(intArray)
  def intArray = {
    val goal = ints.clone
    var i = 0
    val len = goal.length
    while (i < len) { goal(i) = goal(i) * 2; i += 1 }
    goal
  }

  def timeDoubleArray(reps:Int) = run(reps)(doubleArray)
  def doubleArray = {
    val goal = doubles.clone
    var i = 0
    val len = goal.length
    while (i < len) { goal(i) = goal(i) * 2.0; i += 1 }
    goal
  }

  def timeDirectRaster(reps:Int) = run(reps)(directRaster)
  def directRaster = {
    val rcopy = raster.copy
    val goal = rcopy.data
    var i = 0
    val len = goal.length
    while (i < len) { goal(i) = goal(i) * 2; i += 1 }
    rcopy
  }

  def timeIndirectRaster(reps:Int) = run(reps)(indirectRaster)
  def indirectRaster = raster.map(z => z * 2)

  def timeRasterOperation(reps:Int) = run(reps)(rasterOperation)
  def rasterOperation = server.run(op)
}
