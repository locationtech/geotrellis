package geotrellis.benchmark

/*
 * # Caliper API key for jmarcus@azavea.com
 * postUrl: http://microbenchmarks.appspot.com:80/run/
 * apiKey: 3226081d-9776-40f4-a2d7-a1dc99c948c6
*/

import geotrellis._
import geotrellis.data._
import geotrellis.data.png._
import geotrellis.raster.op._
import geotrellis.io._
import geotrellis.raster.op.focal.{Normalize, Aggregated}
import geotrellis.process._
import geotrellis.raster._
import geotrellis.statistics._
import geotrellis.raster.op.local._
import geotrellis.raster.op.tiles._
import geotrellis.statistics.op._
import geotrellis.raster.op.extent.GetRasterExtent
import geotrellis.statistics.op.stat.GetHistogram

import com.google.caliper.Benchmark
import com.google.caliper.Param
import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark

import scala.math.{min, max}
import scala.util.Random

/**
 * Extend this to create an actual benchmarking class.
 */
trait MyBenchmark extends SimpleBenchmark {
  var server:Server = null

  def getRasterExtentOp(name:String, w:Int, h:Int) = {
    val extent = server.run(LoadRasterExtent(name)).extent
    geotrellis.raster.op.extent.GetRasterExtent(extent, w, h)
  }

  /**
   * Loads a given raster with a particular height/width.
   */
  def loadRaster(name:String, w:Int, h:Int) = {
    server.run(LoadRaster(name, getRasterExtentOp(name, w, h)))
  }

  /**
   * Write out a tiled raster in /tmp; return a raster w/ a TiledRasterData
   */
  def createTiledRaster(r:Raster, pixelCols:Int, pixelRows:Int) = {
    val re = r.rasterExtent

    val trd = Tiler.createTiledRasterData(r, pixelCols, pixelRows)
    val tiledArrayRaster = Raster(trd, re)

    val layout = trd.tileLayout 
    Tiler.writeTiles(trd, re, "benchmark_raster", "/tmp")
    val tileSetRD = TileSetRasterData("/tmp", "benchmark_raster", TypeInt, layout, re, server)

    val tiledRaster = Raster(tileSetRD, re)

    val d = r.data.asArray.getOrElse(sys.error("argh"))
    val lazyRasterData = LazyTiledWrapper(d, layout)
    val lazyRaster = Raster(lazyRasterData, re)

    (tiledRaster, tiledArrayRaster, lazyRaster)
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
  def run(reps:Int)(f: => Unit) = {
    var i = 0
    while (i < reps) { f; i += 1 }
  }
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
  //@Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  @Param(Array("2048"))
  var size:Int = 0

  var ints:Array[Int] = null
  var doubles:Array[Double] = null
  var raster:Raster = null
  var bitData:BitArrayRasterData = null
  var byteData:ByteArrayRasterData = null
  var shortData:ShortArrayRasterData = null

  var mc:Op[Raster] = null
  var mcCustomWithInt:Op[Raster] = null
  var mcMapSugar:Op[Raster] = null
  var mcMapIfSetSugar:Op[Raster] = null
  var mcMapIfSetSugarWithLiteral:Op[Raster] = null
  var mcMapIfSet:Op[Raster] = null
  var mcWhileLoop:Op[Raster] = null

  override def setUp() {
    server = initServer()
    val len = size * size
    ints = init(len)(Random.nextInt)
    doubles = init(len)(Random.nextDouble)
    val re = RasterExtent(Extent(0, 0, size, size), 1.0, 1.0, size, size)
    raster = Raster(init(len)(Random.nextInt), re)

    bitData = new BitArrayRasterData(init((len + 7) / 8)(Random.nextInt.toByte), size, size)
    byteData = new ByteArrayRasterData(init(len)(Random.nextInt.toByte), size, size)
    shortData = new ShortArrayRasterData(init(len)(Random.nextInt.toShort), size, size)

    mc = MultiplyConstant(raster, 2)
    mcCustomWithInt = MultiplyConstantCustomWithInt(raster, 2)
    mcMapIfSet = MultiplyConstantMapIfSet(raster, 2)
    mcMapSugar = MultiplyConstantMapSugar(raster, 2)
    mcMapIfSetSugar = MultiplyConstantMapIfSetSugar(raster, 2)
    mcMapIfSetSugarWithLiteral = MultiplyConstantMapIfSetSugarWithLiteral(raster, 2)
    mcWhileLoop = MultiplyConstantWhileLoop(raster, 2)
  }

  def timeIntArrayWhileLoop(reps:Int) = run(reps)(intArrayWhileLoop)
  def intArrayWhileLoop = {
    val goal = ints.clone
    var i = 0
    val len = goal.length
    while (i < len) {
      val z = goal(i)
      if (z != NODATA) goal(i) = z * 2
      i += 1
    }
    goal
  }
  
  def timeDoubleArrayWhileLoop(reps:Int) = run(reps)(doubleArrayWhileLoop)
  def doubleArrayWhileLoop = {
    val goal = doubles.clone
    var i = 0
    val len = goal.length
    while (i < len) {
      val z = goal(i)
      if (z != NODATA) goal(i) = z * 2.0
      i += 1
    }
    goal
  }
  
  def timeRasterWhileLoop(reps:Int) = run(reps)(rasterWhileLoop)
  def rasterWhileLoop = {
    val rcopy = raster.copy
    val goal = rcopy.data.mutable.getOrElse(sys.error("argh"))

    var i = 0
    val len = goal.length
    while (i < len) {
      val z = goal(i)
      if (z != NODATA) goal(i) = goal(i) * 2
      i += 1
    }
    rcopy
  }
  
  def timeRasterMap(reps:Int) = run(reps)(rasterMap)
  def rasterMap = raster.map(z => if (z != NODATA) z * 2 else NODATA)

  def timeRasterMapIfSet(reps:Int) = run(reps)(rasterMapIfSet)
  def rasterMapIfSet = raster.mapIfSet(z => z * 2)

  // xyz
  def timeBitDataWhileLoop(reps:Int) = run(reps)(bitDataWhileLoop)
  def bitDataWhileLoop = {
    val data = bitData.copy
    var i = 0
    val len = data.length
    while (i < len) {
      val z = data(i)
      if (z != NODATA) data(i) = data(i) * 2
      i += 1
    }
    data
  }

  def timeBitDataMap(reps:Int) = run(reps)(bitDataMap)
  def bitDataMap = bitData.map(z => if (z != NODATA) z * 2 else NODATA)

  def timeByteDataWhileLoop(reps:Int) = run(reps)(byteDataWhileLoop)
  def byteDataWhileLoop = {
    val data = byteData.copy
    var i = 0
    val len = data.length
    while (i < len) {
      val z = data(i)
      if (z != NODATA) data(i) = data(i) * 2
      i += 1
    }
    data
  }
  
  def timeByteDataMap(reps:Int) = run(reps)(byteDataMap)
  def byteDataMap = byteData.map(z => if (z != NODATA) z * 2 else NODATA)

  def timeShortDataWhileLoop(reps:Int) = run(reps)(shortDataWhileLoop)
  def shortDataWhileLoop = {
    val data = shortData.copy
    var i = 0
    val len = data.length
    while (i < len) {
      val z = data(i)
      if (z != NODATA) data(i) = data(i) * 2
      i += 1
    }
    data
  }
  
  def timeShortDataMap(reps:Int) = run(reps)(shortDataMap)
  def shortDataMap = shortData.map(z => if (z != NODATA) z * 2 else NODATA)
  // xyz
  
  def timeRasterOperationUnary(reps:Int) = run(reps)(rasterOperationUnary)
  def rasterOperationUnary = server.run(mc)
  
  def timeRasterOperationCustomWithInt(reps:Int) = run(reps)(rasterOperationCustomWithInt)
  def rasterOperationCustomWithInt = server.run(mcCustomWithInt)
  
  def timeRasterOperationMapSugar(reps:Int) = run(reps)(rasterOperationMapSugar)
  def rasterOperationMapSugar = server.run(mcMapSugar)

  def timeRasterOperationMapIfSetSugar(reps:Int) = run(reps)(rasterOperationMapIfSetSugar)
  def rasterOperationMapIfSetSugar = server.run(mcMapIfSetSugar)

  def timeRasterOperationMapIfSetSugarWithLiteral(reps:Int) = run(reps)(rasterOperationMapIfSetSugarWithLiteral)
  def rasterOperationMapIfSetSugarWithLiteral = server.run(mcMapIfSetSugarWithLiteral)

  def timeRasterOperationMapIfSet(reps:Int) = run(reps)(rasterOperationMapIfSet)
  def rasterOperationMapIfSet = server.run(mcMapIfSet)

  def timeRasterOperationWhileLoop(reps:Int) = run(reps)(rasterOperationWhileLoop)
  def rasterOperationWhileLoop = server.run(mcWhileLoop)
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
    val h = GetHistogram(rasterOp, 101) 
    val breaksOp = stat.GetColorBreaks(h, colors)
    op = RenderPng(rasterOp, breaksOp, h, 0)
  }

  def timeWeightedOverlay(reps:Int) = run(reps)(weightedOverlay)
  def weightedOverlay = server.run(op)
}

object AddRasters extends MyRunner(classOf[AddRasters])
class AddRasters extends MyBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  var size:Int = 0

  var op:Op[Raster] = null

  override def setUp() {
    server = initServer()
    val r:Raster = loadRaster("SBN_farm_mkt", size, size)
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

  var op:Op[Raster] = null

  override def setUp() {
    server = initServer()
    val r:Raster = loadRaster("SBN_farm_mkt", size, size)
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

  var op:Op[Raster] = null

  override def setUp() {
    server = initServer()
    val r:Raster = loadRaster("SBN_farm_mkt", size, size)
    op = AddConstant(r, 13)
  }

  def timeConstantAdd(reps:Int) = run(reps)(constantAdd)
  def constantAdd = server.run(op)
}

object BigMinTiled {
  def main(args:Array[String]) = {
    val tileN = args(0).toInt
    val test = new BigMinTiled
    test.tileN = tileN
    val runs = 2
    println("Starting setup.")
    test.setUp()
    for (i <- 0 until 3) {
      println("Starting test.")
      val start = System.currentTimeMillis
      //test.min
      test.histogram
      val elapsed = System.currentTimeMillis - start
      println("Test complete: %d millis" format (elapsed))
      val cells = tileN * 2000L * tileN * 2000L
      val rate = cells / elapsed / 1000L
      println("Rate: %d k/ms" format (rate) )
    }

    test.server.shutdown
  }
}

class BigMinTiled extends MyBenchmark{ 
  var tiledMinOp:Op[Int] = null
  var tiledHistogramOp:Op[Histogram] = null

  var tileN = 10

  override def setUp() {
    server = initServer()
    val size = tileN * 2000
    println("Setting up raster of size %d x %d." format (size, size))
    val layout = TileLayout(tileN,tileN,2000,2000)
    val e = Extent(0.0, 0.0, (tileN * 2000.0), (tileN * 2000.0))
    val re = RasterExtent(e, 1.0, 1.0, tileN * 2000, tileN * 2000)
    val tileSetRD = TileSetRasterData("/tmp", "big", TypeByte, layout, re, server)
    val raster = Raster(tileSetRD, re)
    tiledMinOp = BTileMin(Add(AddConstant(raster,2), raster))
    //tiledHistogramOp = BTileHistogram(AddConstant(raster,2))
    tiledHistogramOp = BTileHistogram(raster)
  }

  def timeMin(reps:Int) = run(reps)(min)
  def min = { 
    val min = server.run(tiledMinOp)
    println("Found min: %d" format (min))
  }
  
  def timeHistogram(reps:Int) = run(reps)(histogram)
  def histogram = {
    val h = server.run(tiledHistogramOp)
    println("Found histogram: %s" format (h.toJSON))
  }
}


object MinTiled extends MyRunner(classOf[MinTiled])
class MinTiled extends MyBenchmark {
  @Param(Array("4096"))
  var size:Int = 0

  //@Param(Array("256", "512"))
  var pixels:Int = 1024

  var tiledOp:Op[Int] = null
  var tiledArrayOp:Op[Int] = null
  var tiledLazyOp:Op[Int] = null
  var rawOp:Op[Raster] = null

  var normalUntiledOp:Op[Int] = null
  var normalTiledOp:Op[Int] = null 

  //def makeOp(r:Op[Raster]):Operation[Raster] = r
  def makeOp(r:Op[Raster]) = Add(AddConstant(r, 5),r)

  override def setUp() {
    server = initServer()
    val r:Raster = loadRaster("SBN_farm_mkt", size, size)
    val (tiledRaster, tiledArrayRaster, lazyRaster) = createTiledRaster(r, pixels, pixels)

    tiledOp = BTileMin(makeOp(tiledRaster))
    tiledArrayOp = BTileMin(makeOp(tiledArrayRaster))
    tiledLazyOp = TileMin(makeOp(lazyRaster))

    // run on a normal raster
    normalUntiledOp = UntiledMin(makeOp(r))
    normalTiledOp = BTileMin(makeOp(r))

    rawOp = Force(makeOp(r))
  }

  //def timeTiledMin(reps:Int) = run(reps)(tiledMin)
  def tiledMin = server.run(tiledOp)
 
  def timeTiledArrayMinOp(reps:Int) = run(reps)(tiledArrayMin)
  def tiledArrayMin = server.run(tiledArrayOp)

  //def timeTiledLazyOp(reps:Int) = run(reps)(tiledLazyMin)
  // def tiledLazyMin = server.run(tiledLazyOp)

  def timeNormalUntiledOp(reps:Int) = run(reps)(runNormalUntiledOp)
  def runNormalUntiledOp = server.run(normalUntiledOp)

  def timeRawOp(reps:Int) = run(reps)(runRawOp)
  def runRawOp = server.run(rawOp)

  def timeNormalTiledOp(reps:Int) = run(reps)(runNormalTiledOp)
  def runNormalTiledOp = server.run(normalTiledOp)
}


object HistogramTiled extends MyRunner(classOf[HistogramTiled])
class HistogramTiled extends MyBenchmark {
  @Param(Array("6000"))
  var size:Int = 0

  //@Param(Array("256", "512"))
  var pixels:Int = 1500

  var tiledOp:Op[Histogram] = null
  var tiledArrayOp:Op[Histogram] = null
  var tiledArrayForceOp:Op[Histogram] = null

  var tiledLazyOp:Op[Histogram] = null
  var rawOp:Op[Raster] = null

  var normalUntiledOp:Op[Histogram] = null
  var normalUntiledLazyOp:Op[Histogram] = null

  var normalTiledOp:Op[Histogram] = null 

  //def makeOp(r:Raster) = Add(AddConstant(r, 5), r)
  def makeOp(r:Raster) = Add(r, r)
  //def makeOp(r:Raster) = AddConstant(r, 5)
  //def makeOp(r:Raster) = Literal(r)

  override def setUp() {
    server = initServer()
    val r:Raster = loadRaster("SBN_farm_mkt", size, size)
    val (tiledRaster, tiledArrayRaster, lazyRaster) = createTiledRaster(r, pixels, pixels)

    tiledOp = BTileHistogram(makeOp(tiledRaster))
    tiledArrayOp = BTileHistogram(makeOp(tiledArrayRaster))
    tiledArrayForceOp = BTileForceHistogram(makeOp(tiledArrayRaster))
    tiledLazyOp = TileHistogram(makeOp(lazyRaster))

    // run on a normal raster
    normalUntiledOp = BUntiledHistogram(makeOp(r))
    normalUntiledLazyOp = BUntiledHistogram(makeOp(r.defer))
    
    normalTiledOp = BTileHistogram(makeOp(r.defer))

    rawOp = Force(makeOp(r))
  }

  def timeTiledHistogram(reps:Int) = run(reps)(tiledHistogram)
  def tiledHistogram = server.run(tiledOp)
  
  def timeRawOp(reps:Int) = run(reps)(runRawOp)
  def runRawOp = server.run(rawOp)
  
  def timeNormalUntiledOp(reps:Int) = run(reps)(runNormalUntiledOp)
  def runNormalUntiledOp = server.run(normalUntiledOp)
  
  def timeNormalUntiledLazyOp(reps:Int) = run(reps)(runNormalUntiledLazyOp)
  def runNormalUntiledLazyOp = server.run(normalUntiledLazyOp)
  
  def timeTiledArrayHistogramOp(reps:Int) = run(reps)(tiledArrayHistogram)
  def tiledArrayHistogram = server.run(tiledArrayOp)

  def timeTiledArrayForce(reps:Int) = run(reps)(tiledArrayForce)
  def tiledArrayForce = server.run(tiledArrayForceOp)

  def timeTiledLazyOp(reps:Int) = run(reps)(tiledLazyHistogram)
  def tiledLazyHistogram = server.run(tiledLazyOp)
  
  def timeNormalTiledOp(reps:Int) = run(reps)(runNormalTiledOp)
  def runNormalTiledOp = server.run(normalTiledOp)
}


object MiniWeightedOverlay extends MyRunner(classOf[MiniWeightedOverlay])
class MiniWeightedOverlay extends MyBenchmark {
  //@Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "8192", "10000"))
  @Param(Array("100", "1000", "10000"))
  var size:Int = 0
  
  var op:Op[Raster] = null

  var strictOp:Op[Raster] = null
  var lazyOp:Op[Raster] = null

  override def setUp() {
    server = initServer()
    val r1:Raster = loadRaster("SBN_farm_mkt", size, size)
    val r2:Raster = loadRaster("SBN_RR_stops_walk", size, size)

    op = AddOld(MultiplyConstant(r1, 5), MultiplyConstant(r2, 2))

    strictOp = Add(MultiplyConstantMapSugar(r1, 5), MultiplyConstantMapSugar(r2, 2))
    lazyOp = Force(Add(MultiplyConstantMapSugar(r1.defer, 5), MultiplyConstantMapSugar(r2.defer, 2)))
  }

  def timeMiniWeightedOverlay(reps:Int) = run(reps)(miniWeightedOverlay)
  def miniWeightedOverlay = server.run(op)

  def timeMiniWeightedOverlayStrict(reps:Int) = run(reps)(miniWeightedOverlayStrict)
  def miniWeightedOverlayStrict = server.run(strictOp)

  def timeMiniWeightedOverlayLazy(reps:Int) = run(reps)(miniWeightedOverlayLazy)
  def miniWeightedOverlayLazy = server.run(lazyOp)
}


object NewAddOperations extends MyRunner(classOf[NewAddOperations])
class NewAddOperations extends MyBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "8192", "10000"))
  var size:Int = 0
  
  var strictOld:Op[Raster] = null
  var strictNew:Op[Raster] = null
  var lazyNew:Op[Raster] = null

  override def setUp() {
    server = initServer()

    val r1:Raster = loadRaster("SBN_farm_mkt", size, size)
    val r2:Raster = loadRaster("SBN_RR_stops_walk", size, size)

    val l1:Raster = r1.defer
    val l2:Raster = r2.defer

    strictOld = Force(AddOld(AddOld(r1, r2), AddOld(r1, r2)))
    strictNew = Force(Add(Add(r1, r2), Add(r1, r2)))
    lazyNew = Force(Add(Add(l1, l2), Add(l1, l2)))
  }

  def timeStrictOld(reps:Int) = run(reps)(runStrictOld)
  def runStrictOld = server.run(strictOld)

  def timeStrictNew(reps:Int) = run(reps)(runStrictNew)
  def runStrictNew = server.run(strictNew)

  def timeLazyNew(reps:Int) = run(reps)(runLazyNew)
  def runLazyNew = server.run(lazyNew)
}


object LazyIteration extends MyRunner(classOf[LazyIteration])
class LazyIteration extends MyBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "8192", "10000"))
  var size:Int = 0
  
  @Param(Array("1", "2", "3", "4"))
  var iterations:Int = 0

  var simpleOp:Op[Raster] = null
  var mediumOp:Op[Raster] = null
  var complexOp:Op[Raster] = null

  override def setUp() {
    server = initServer()

    val r1:Raster = loadRaster("SBN_farm_mkt", size, size).defer
    val r2:Raster = loadRaster("SBN_RR_stops_walk", size, size).defer
    val r3:Raster = loadRaster("SBN_inc_percap", size, size).defer

    simpleOp = AddConstant(r1, 6) 
    mediumOp = Add(MultiplyConstant(r1, 2), MultiplyConstant(r2, 3))
    complexOp = Add(DivideConstant(Add(MultiplyConstant(r1, 2),
                                       MultiplyConstant(r2, 3),
                                       MultiplyConstant(r3, 4)), 9), mediumOp)
  }

  def timeSimpleOpLazyIteration(reps:Int) = run(reps)(simpleOpLazyIteration)
  def simpleOpLazyIteration = {
    val r = server.run(simpleOp)
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }

  def timeSimpleOpStrictIteration(reps:Int) = run(reps)(simpleOpStrictIteration)
  def simpleOpStrictIteration = {
    val r = server.run(simpleOp).force
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }

  def timeMediumOpLazyIteration(reps:Int) = run(reps)(mediumOpLazyIteration)
  def mediumOpLazyIteration = {
    val r = server.run(mediumOp)
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }

  def timeMediumOpStrictIteration(reps:Int) = run(reps)(mediumOpStrictIteration)
  def mediumOpStrictIteration = {
    val r = server.run(mediumOp).force
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }

  def timeComplexOpLazyIteration(reps:Int) = run(reps)(complexOpLazyIteration)
  def complexOpLazyIteration = {
    val r = server.run(complexOp)
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }

  def timeComplexOpStrictIteration(reps:Int) = run(reps)(complexOpStrictIteration)
  def complexOpStrictIteration = {
    val r = server.run(complexOp).force
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }
}


object RasterForeach extends MyRunner(classOf[RasterForeach])
class RasterForeach extends MyBenchmark {
  //@Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "8192", "10000"))
  @Param(Array("64", "128", "256", "512", "1024"))
  var size:Int = 0
  
  var r:Raster = null

  override def setUp() {
    server = initServer()
    r = server.run(loadRaster("SBN_farm_mkt", size, size))
  }

  def timeRasterForeach(reps:Int) = run(reps)(rasterForeach)
  def rasterForeach = {
    var t = 0
    r.foreach(z => t += z)
    t
  }

  def timeRasterWhile(reps:Int) = run(reps)(rasterWhile)
  def rasterWhile = {
    var t = 0
    var i = 0
    val d = r.data.asArray.getOrElse(sys.error("argh"))
    val len = r.length
    while (i < len) {
      t += d(i)
      i += 1
    }
    t
  }
}


object WriteHugeTiledRaster {
  def main(args:Array[String]) {
    if (args.length < 6) {
      println("usage: PATH NAME COLS ROWS PIXELCOLS PIXELROWS")
      println("e.g. /tmp hugeraster 1024 1024 256 256")
    }
    val path = args(0)
    val name = args(1)
    val cols = args(2).toInt
    val rows  = args(3).toInt
    val pixelCols = args(4).toInt
    val pixelRows = args(5).toInt
    println("Creating raster (%d, %d) with tiles (%d, %d)" format (cols, rows,
                                                                   pixelCols, pixelRows))

    // map units = pixels
    val extent = Extent(0, 0, cols, rows)
    val re = RasterExtent(extent, 1.0, 1.0, cols, rows)
    
    Tiler.writeTilesFromFunction(pixelCols, pixelRows, re, name, path, f)
  }
  
  def f(tileCol:Int, tileRow:Int, layout:TileLayout, re:RasterExtent):Raster = {
    val rl = layout.getResolutionLayout(re)
    val tileRasterExtent = rl.getRasterExtent(tileCol,tileRow)

    val value:Byte = ((tileCol % 50) + 1).toByte
    val size = layout.pixelCols * layout.pixelRows
    val a = ByteArrayRasterData(Array.fill[Byte](size)(value),
                                layout.pixelCols, layout.pixelRows)
    Raster(a,tileRasterExtent)
  }
}



object FocalOperations extends MyRunner(classOf[FocalOperations])
class FocalOperations extends MyBenchmark {
  
  var r:Raster = null

  override def setUp() {
    server = TestServer()

    val scale = 1
    //val scale = 0.1 // used to allow naive version to run fast enough

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0 / scale, 75.0 / scale,
                          (2101 * scale).toInt, (1723 * scale).toInt)
    val path = "src/main/resources/sbn/SBN_inc_percap.arg"
    r = server.run(io.LoadFile(path, re))
  }

  def timeCursorMeanSquare1(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(1))))
  def timeCursorMeanSquare2(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(2))))
  def timeCursorMeanSquare3(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(3))))
  def timeCursorMeanSquare5(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(5))))
  def timeCursorMeanSquare7(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(7))))
  def timeCursorMeanSquare8(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(8))))
  def timeCursorMeanSquare13(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(13))))

  def timeMeanSquare1(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(1))))
  def timeMeanSquare2(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(2))))
  def timeMeanSquare3(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(3))))
  def timeMeanSquare5(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(5))))
  def timeMeanSquare7(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(7))))
  def timeMeanSquare8(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(8))))
  def timeMeanSquare13(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(13))))

  def timeFastMean1(reps:Int) = run(reps)(server.run(FastFocalMean(r, 1)))
  def timeFastMean2(reps:Int) = run(reps)(server.run(FastFocalMean(r, 2)))
  def timeFastMean3(reps:Int) = run(reps)(server.run(FastFocalMean(r, 3)))
  def timeFastMean5(reps:Int) = run(reps)(server.run(FastFocalMean(r, 5)))
  def timeFastMean7(reps:Int) = run(reps)(server.run(FastFocalMean(r, 7)))
  def timeFastMean8(reps:Int) = run(reps)(server.run(FastFocalMean(r, 8)))
  def timeFastMean13(reps:Int) = run(reps)(server.run(FastFocalMean(r, 13)))

  def timeCursorMeanCircle1(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Circle(1))))
  def timeCursorMeanCircle2(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Circle(2))))
  def timeCursorMeanCircle3(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Circle(3))))
  def timeCursorMeanCircle5(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Circle(5))))
  
  def timeMeanCircle1(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(1))))
  def timeMeanCircle2(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(2))))
  def timeMeanCircle3(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(3))))
  def timeMeanCircle5(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(5))))

  def timeHillshade(reps:Int) = run(reps)(server.run(focal.Hillshade(r))) 
}

object FocalMinOperation extends MyRunner(classOf[FocalMinOperation])
class FocalMinOperation extends MyBenchmark {
  
  var r:Raster = null

  override def setUp() {
    server = TestServer()

    val scale = 1
    //val scale = 0.1 // used to allow naive version to run fast enough

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0 / scale, 75.0 / scale,
                          (2101 * scale).toInt, (1723 * scale).toInt)
    val path = "src/main/resources/sbn/SBN_inc_percap.arg"
    r = server.run(io.LoadFile(path, re))
  }

  import geotrellis.raster.op.focal._

  def timeNewMinSquare1(reps:Int) = run(reps)(server.run(Min(r, Square(1))))
  def timeNewMinSquare(reps:Int) = run(reps)(server.run(Min(r, Square(2))))
  def timeNewMinSquare3(reps:Int) = run(reps)(server.run(Min(r, Square(3))))

  def timeOldMinSquared1(reps:Int) = run(reps)(server.run(Min(r, Square(1))))
  def timeOldMinSquared(reps:Int) = run(reps)(server.run(Min(r, Square(2))))
  def timeOldMinSquared3(reps:Int) = run(reps)(server.run(Min(r, Square(3)))) 
   
  def timeNewMinCircle1(reps:Int) = run(reps)(server.run(Min(r, Circle(1))))
  def timeNewMinCircle(reps:Int) = run(reps)(server.run(Min(r, Circle(2))))
  def timeNewMinCircle3(reps:Int) = run(reps)(server.run(Min(r, Circle(3))))

  def timeOldMinCircle1(reps:Int) = run(reps)(server.run(Min(r, Circle(1))))
  def timeOldMinCircle(reps:Int) = run(reps)(server.run(Min(r, Circle(2))))
  def timeOldMinCircle3(reps:Int) = run(reps)(server.run(Min(r, Circle(3))))
}

