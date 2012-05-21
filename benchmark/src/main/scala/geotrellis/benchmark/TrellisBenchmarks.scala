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

import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark

class MiniAddTest(raster:IntRaster) {
  val op = AddConstant(raster, 13)
  def run(reps:Int, server:Server) {
    var r:IntRaster = null
    for (i <- 0 until reps) r = server.run(op)
  }
}

object MiniAddTest {
  def apply(server:Server, name:String, extent:Extent, size:Int) = {
    val e = server.run(LoadRasterExtent(name)).extent
    val r = server.run(LoadRaster(name, BuildRasterExtent(e, size, size)))
    new MiniAddTest(r)
  }
}

class TiledMiniAddTest(raster:IntRaster) {
  val op = ForEachTile(raster)(AddConstant(_, 13))
  def run(reps:Int, server:Server) {
    var r:IntRaster = null
    for (i <- 0 until reps) r = server.run(op)
  }
}

object TiledMiniAddTest {
  def apply(server:Server, name:String, extent:Extent, size:Int, pixels:Int) = {
    val e = server.run(LoadRasterExtentFromFile(name)).extent
    val r = server.run(LoadFile(name, BuildRasterExtent(e, size, size)))
    val t = Tiler.createTileRaster(r, pixels)
    new TiledMiniAddTest(t)
  }
}

class MiniWoTest(raster1:IntRaster, weight1:Int, raster2:IntRaster, weight2:Int) {
  val op = Add(MultiplyConstant(raster1, weight1),
               MultiplyConstant(raster2, weight2))
  def run(reps:Int, server:Server) {
    var r:IntRaster = null
    for (i <- 0 until reps) r = server.run(op)
  }
}

object MiniWoTest {
  def apply(server:Server, pair1:(String, Int), pair2:(String, Int),
            extent:Extent, size:Int) = {
    val (name1, weight1) = pair1
    val (name2, weight2) = pair2

    val e = server.run(LoadRasterExtentFromFile(name1)).extent
    val r1 = server.run(LoadFile(name1, BuildRasterExtent(e, size, size)))
    val r2 = server.run(LoadFile(name2, BuildRasterExtent(e, size, size)))
    new MiniWoTest(r1, weight1, r2, weight2)
  }
}

class WoTest(size:Int, extent:Extent, pairs:Seq[(String, Int)],
                  total:Int, colors:Array[Int]) {

  val re = BuildRasterExtent(extent, size, size)

  def buildScaleRasterOp(name:String, weight:Int) = {
    MultiplyConstant(LoadRaster(name, re), weight)
  }

  def buildWoOp = {
    val rs = pairs.map { case (p, w) => buildScaleRasterOp(p, w) }.toArray
    Normalize(DivideConstant(Add(rs: _*), total), (1, 100))
  }

  def buildColorBreaksOp(r:Op[IntRaster]) = {
    FindColorBreaks(BuildArrayHistogram(r, 101), colors)
  }

  def buildPNGOp(r:Op[IntRaster]) = {
    RenderPNG(r, buildColorBreaksOp(r), 0, true)
  }

  val weight:Op[IntRaster] = buildWoOp
  val breaks:Op[ColorBreaks] = buildColorBreaksOp(weight)
  val render:Op[Array[Byte]] = buildPNGOp(weight)

  def run(reps:Int, server:Server) {
    var d:Array[Byte] = null
    for (i <- 0 until reps) d = server.run(render)
  }
}

/**
 *
 */
class AddRastersTest(raster:IntRaster) {
  val r1 = AddConstant(raster, 1)
  val r2 = AddConstant(raster, 2)
  val r3 = AddConstant(raster, 3)
  val r4 = AddConstant(raster, 4)
  val r5 = AddConstant(raster, 5)

  val op = Add(r1, r2, r3, r4, r5)

  def run(reps:Int, server:Server) = {
    var r:IntRaster = null
    for (i <- 0 until reps) r = server.run(op)
    r
  }
}

object AddRastersTest {
  def apply(server:Server, name:String, extent:Extent, size:Int) = {
    val e = server.run(LoadRasterExtentFromFile(name)).extent
    val r = server.run(LoadRaster(name, BuildRasterExtent(e, size, size)))
    new AddRastersTest(r)
  }
}


/**
 *
 */
class SubtractRastersTest(raster1:IntRaster, raster2:IntRaster) {
  val r1 = AddConstant(raster1, 10)
  val r2 = AddConstant(raster2, 2)

  val op = Subtract(r1, r2)

  def run(reps:Int, server:Server) = {
    var r:IntRaster = null
    for (i <- 0 until reps) r = server.run(op)
    r
  }
}

object SubtractRastersTest {
  def apply(server:Server, name:String, extent:Extent, size:Int) = {
    val e = server.run(LoadRasterExtentFromFile(name)).extent
    val r1 = server.run(LoadRaster(name, BuildRasterExtent(e, size, size)))
    val r2 = server.run(LoadRaster(name, BuildRasterExtent(e, size, size)))
    new SubtractRastersTest(r1, r2)
  }
}


//TODO: add benchmarks for WO at 2048,4096,8192,etc
class WoBenchmark extends GeoTrellisBenchmark {
  var s64:WoTest = null
  var s128:WoTest = null
  var s256:WoTest = null
  var s512:WoTest = null
  var s1024:WoTest = null

  override def testSetup() {
    val name = pairs(0)._1
    val extent = server.run(LoadRasterExtent(name)).extent

    s64 = new WoTest(64, extent, pairs, total, colors)
    s128 = new WoTest(128, extent, pairs, total, colors)
    s256 = new WoTest(256, extent, pairs, total, colors)
    s512 = new WoTest(512, extent, pairs, total, colors)
    s1024 = new WoTest(1024, extent, pairs, total, colors)
  }
  def timeWeightedOverlayPNG_64(reps:Int) = s64.run(reps, server)
  def timeWeightedOverlayPNG_128(reps:Int) = s128.run(reps, server)
  def timeWeightedOverlayPNG_256(reps:Int) = s256.run(reps, server)
  def timeWeightedOverlayPNG_512(reps:Int) = s512.run(reps, server)
  def timeWeightedOverlayPNG_1024(reps:Int) = s1024.run(reps, server)
}

class BasicWoBenchmark extends GeoTrellisBenchmark {
  var m100:MiniWoTest = null
  var m1000:MiniWoTest = null

  override def testSetup() {
    val name = pairs(0)._1
    val extent = server.run(LoadRasterExtent(name)).extent

    m100 = MiniWoTest(server, pairs(0), pairs(1), extent, 100)
    m1000 = MiniWoTest(server, pairs(0), pairs(1), extent, 1000)
  }

  def timeBasicWeightedOverlay_100(reps:Int) = m100.run(reps, server)
  def timeBasicWeightedOverlay_1000(reps:Int) = m1000.run(reps, server)
}

class AddConstantBenchmark extends GeoTrellisBenchmark {
  var a64:MiniAddTest = null
  var a128:MiniAddTest = null
  var a256:MiniAddTest = null
  var a512:MiniAddTest = null
  var a1024:MiniAddTest = null

  override def testSetup() {
    val name = pairs(0)._1
    val extent = server.run(LoadRasterExtent(name)).extent
    
    a64 = MiniAddTest(server, name, extent, 64)
    a128 = MiniAddTest(server, name, extent, 128)
    a256 = MiniAddTest(server, name, extent, 256)
    a512 = MiniAddTest(server, name, extent, 512)
    a1024 = MiniAddTest(server, name, extent, 1024)
  }

  def timeAddConstant_64(reps:Int) = a64.run(reps, server)
  def timeAddConstant_128(reps:Int) = a128.run(reps, server)
  def timeAddConstant_256(reps:Int) = a256.run(reps, server)
  def timeAddConstant_512(reps:Int) = a512.run(reps, server)
  def timeAddConstant_1024(reps:Int) = a1024.run(reps, server)

}

class TiledAddConstantBenchmark extends GeoTrellisBenchmark {
  var t_a4096:TiledMiniAddTest = null
  var t_a8192_512:TiledMiniAddTest = null
  override def testSetup() {
    val name = pairs(0)._1
    val extent = server.run(LoadRasterExtent(name)).extent
    t_a4096 = TiledMiniAddTest(server, name, extent, 4096, 512)
    t_a8192_512 = TiledMiniAddTest(server, name, extent, 8192, 512)
  }
  def timeTiledAddConstant_4086(reps:Int) = t_a4096.run(reps, server)
  def timeTiledAddConstant_8196_512(reps:Int) = t_a8192_512.run(reps, server)
}

class AddRastersBenchmark extends GeoTrellisBenchmark {
  var ma256:AddRastersTest = null
  var ma512:AddRastersTest = null
  var ma1024:AddRastersTest = null
  override def testSetup() {
    val name = pairs(0)._1
    val extent = server.run(LoadRasterExtent(name)).extent
    ma256 = AddRastersTest(server, name, extent, 256)
    ma512 = AddRastersTest(server, name, extent, 512)
    ma1024 = AddRastersTest(server, name, extent, 1024)
  }
  def timeAddRasters_256(reps:Int) = ma256.run(reps, server)
  def timeAddRasters_512(reps:Int) = ma512.run(reps, server)
  def timeAddRasters_1024(reps:Int) = ma1024.run(reps, server)
}

class SubtractRastersBenchmark extends GeoTrellisBenchmark {
  var sb256:SubtractRastersTest = null
  var sb512:SubtractRastersTest = null
  var sb1024:SubtractRastersTest = null
  override def testSetup() {
    val name = pairs(0)._1
    val extent = server.run(LoadRasterExtent(name)).extent
    sb256 = SubtractRastersTest(server, name, extent, 256)
    sb512 = SubtractRastersTest(server, name, extent, 512)
    sb1024 = SubtractRastersTest(server, name, extent, 1024)
  }
 
  def timeSubtractRasters_256(reps:Int) = sb256.run(reps, server)
  def timeSubtractRasters_512(reps:Int) = sb512.run(reps, server)
  def timeSubtractRasters_1024(reps:Int) = sb1024.run(reps, server)
}

abstract class GeoTrellisBenchmark extends SimpleBenchmark {
  val pairs = List(("SBN_farm_mkt", 2), ("SBN_RR_stops_walk", 1), ("SBN_inc_percap", 5), ("SBN_street_den_1k", 2))
  val total = pairs.map(_._2).sum
  val colors = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)

  var server:Server = null
  def testSetup() {}

  override def setUp() {
    val catalogPath = "src/test/resources/demo-catalog.json"
    val catalog = Catalog.fromPath(catalogPath)
    server = Server("demo", catalog)

    testSetup()
  }

 }

object BenchmarkWo {
  def main(args:Array[String]) {
    println("starting weighted overlay -> png benchmarks...")
    Runner.main(classOf[WoBenchmark], args:_*)
    println("completed benchmarks.")
  }
}

object BenchmarkBasicWo {
  def main(args:Array[String]) {
    println("starting basic weighted overlay benchmarks.")
    Runner.main(classOf[BasicWoBenchmark], args:_*)
    println("completed benchmarks.")
  }
}

object BenchmarkAddConstant {
  def main(args:Array[String]) {
    println("starting benchmarks.")
    Runner.main(classOf[AddConstantBenchmark], args:_*)
    println("completed benchmarks.")
  }
}

object BenchmarkTiledAddConstant {
  def main(args:Array[String]) {
    println("starting benchmarks.")
    Runner.main(classOf[AddConstantBenchmark], args:_*)
    println("completed benchmarks.")
  }
}

object BenchmarkAddRasters {
  def main(args:Array[String]) {
    println("starting benchmarks.")
    Runner.main(classOf[AddConstantBenchmark], args:_*)
    println("completed benchmarks.")
  }
}

object BenchmarkSubtractRasters {
  def main(args:Array[String]) {
    println("starting benchmarks.")
    Runner.main(classOf[AddConstantBenchmark], args:_*)
    println("completed benchmarks.")
  }
}


