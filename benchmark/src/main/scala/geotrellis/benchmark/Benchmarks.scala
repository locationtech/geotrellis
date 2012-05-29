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
  var server:Server = null

  def getRasterExtentOp(name:String, w:Int, h:Int) = {
    val extent = server.run(LoadRasterExtent(name)).extent
    BuildRasterExtent(extent, w, h)
  }

  /**
   * Loads a given raster with a particular height/width.
   */
  def loadRaster(name:String, w:Int, h:Int) = {
    server.run(LoadRaster(name, getRasterExtentOp(name, w, h)))
  }

  /**
   * Load a server with the GeoTrellis benchmarking catalog.
   */
  def initServer():Server = {
    val catalog = Catalog.fromPath("src/main/resources/catalog.json")
    Server("demo", catalog)
  }

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
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  var size:Int = 0

  var ints:Array[Int] = null
  var doubles:Array[Double] = null
  var raster:IntRaster = null
  var op:Op[IntRaster] = null

  override def setUp() {
    server = initServer()
    val len = size * size
    ints = init(len)(Random.nextInt)
    doubles = init(len)(Random.nextDouble)
    val re = RasterExtent(Extent(0, 0, size, size), 1.0, 1.0, size, size)
    raster = IntRaster(init(len)(Random.nextInt), re)
    op = MultiplyConstant(raster, 2)
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

object WeightedOverlay extends MyRunner(classOf[WeightedOverlay])
class WeightedOverlay extends MyBenchmark {
  val n = 4
  val names = Array("SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k")
  val weights = Array(2, 1, 5, 2)
  val colors = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)

  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  var size:Int = 0

  var op:Op[Array[Byte]] = null

  override def setUp() {
    server = initServer()
    val reOp = getRasterExtentOp(names(0), size, size)
    val total = weights.sum
    val rs = (0 until n).map(i => MultiplyConstant(LoadRaster(names(i), reOp), weights(i)))
    val rasterOp = Normalize(DivideConstant(Add(rs: _*), total), (1, 100))
    val breaksOp = FindColorBreaks(BuildArrayHistogram(rasterOp, 101), colors)
    op = RenderPNG(rasterOp, breaksOp, 0, true)
  }

  def timeWeightedOverlay(reps:Int) = run(reps)(weightedOverlay)
  def weightedOverlay = server.run(op)
}

object AddRasters extends MyRunner(classOf[AddRasters])
class AddRasters extends MyBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  var size:Int = 0

  var op:Op[IntRaster] = null

  override def setUp() {
    server = initServer()
    val r:IntRaster = loadRaster("SBN_farm_mkt", size, size)
    val r1 = AddConstant(r, 1)
    val r2 = AddConstant(r, 2)
    val r3 = AddConstant(r, 3)
    val r4 = AddConstant(r, 4)
    val r5 = AddConstant(r, 5)
    op = Add(r1, r2, r3, r4, r5)
  }

  def timeAddRasters(reps:Int) = run(reps)(addRasters)
  def addRasters = server.run(op)
}

object SubtractRasters extends MyRunner(classOf[SubtractRasters])
class SubtractRasters extends MyBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  var size:Int = 0

  var op:Op[IntRaster] = null

  override def setUp() {
    server = initServer()
    val r:IntRaster = loadRaster("SBN_farm_mkt", size, size)
    val r1 = MultiplyConstant(r, 2)
    val r2 = AddConstant(r, 2)
    op = Subtract(r1, r2)
  }

  def timeSubtractRasters(reps:Int) = run(reps)(subtractRasters)
  def subtractRasters = server.run(op)
}


object ConstantAdd extends MyRunner(classOf[ConstantAdd])
class ConstantAdd extends MyBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "10000"))
  var size:Int = 0

  var op:Op[IntRaster] = null

  override def setUp() {
    server = initServer()
    val r:IntRaster = loadRaster("SBN_farm_mkt", size, size)
    op = AddConstant(r, 13)
  }

  def timeConstantAdd(reps:Int) = run(reps)(constantAdd)
  def constantAdd = server.run(op)
}


object TiledConstantAdd extends MyRunner(classOf[TiledConstantAdd])
class TiledConstantAdd extends MyBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  var size:Int = 0

  @Param(Array("64", "128", "256", "512"))
  var pixels:Int = 0
  
  var op:Op[IntRaster] = null

  override def setUp() {
    server = initServer()
    val r:IntRaster = loadRaster("SBN_farm_mkt", size, size)
    val t = Tiler.createTileRaster(r, pixels)
    op = ForEachTile(t)(AddConstant(_, 13))
  }

  def timeTiledConstantAdd(reps:Int) = run(reps)(tiledConstantAdd)
  def tiledConstantAdd = server.run(op)
}


object MiniWeightedOverlay extends MyRunner(classOf[MiniWeightedOverlay])
class MiniWeightedOverlay extends MyBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "8192", "10000"))
  var size:Int = 0
  
  var op:Op[IntRaster] = null

  override def setUp() {
    server = initServer()
    val r1:IntRaster = loadRaster("SBN_farm_mkt", size, size)
    val r2:IntRaster = loadRaster("SBN_RR_stops_walk", size, size)
    op = Add(MultiplyConstant(r1, 5), MultiplyConstant(r2, 2))
  }

  def timeMiniWeightedOverlay(reps:Int) = run(reps)(miniWeightedOverlay)
  def miniWeightedOverlay = server.run(op)
}
