package trellis.benchmark

/*
 * # Caliper API key for jmarcus@azavea.com
 * postUrl: http://microbenchmarks.appspot.com:80/run/
 * apiKey: 3226081d-9776-40f4-a2d7-a1dc99c948c6
*/

import trellis._
import trellis.data._
import trellis.operation._
import trellis.operation.render.png.RenderPNG2
import trellis.process._
import trellis.raster._

import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark

class MiniAddBenchmark(raster:IntRaster) {
  val op = AddConstant(raster, 13)
  def run(reps:Int, server:Server) {
    var r:IntRaster = null
    for (i <- 0 until reps) r = server.run(op)
  }
}

object MiniAddBenchmark {
  def apply(server:Server, path:String, extent:Extent, size:Int) = {
    val e = server.run(LoadRasterExtentFromFile(path)).extent
    val r = server.run(LoadFile(path, BuildRasterExtent2(e, size, size)))
    new MiniAddBenchmark(r)
  }
}

class MiniWoBenchmark(raster1:IntRaster, weight1:Int, raster2:IntRaster, weight2:Int) {
  val op = Add(MultiplyConstant(raster1, weight1),
               MultiplyConstant(raster2, weight2))
  def run(reps:Int, server:Server) {
    var r:IntRaster = null
    for (i <- 0 until reps) r = server.run(op)
  }
}

object MiniWoBenchmark {
  def apply(server:Server, pair1:(String, Int), pair2:(String, Int),
            extent:Extent, size:Int) = {
    val (path1, weight1) = pair1
    val (path2, weight2) = pair2

    val e = server.run(LoadRasterExtentFromFile(path1)).extent
    val r1 = server.run(LoadFile(path1, BuildRasterExtent2(e, size, size)))
    val r2 = server.run(LoadFile(path2, BuildRasterExtent2(e, size, size)))
    new MiniWoBenchmark(r1, weight1, r2, weight2)
  }
}

class WoBenchmark(size:Int, extent:Extent, pairs:Seq[(String, Int)],
                  total:Int, colors:Array[Int]) {

  val re = BuildRasterExtent2(extent, size, size)

  def buildScaleRasterOp(path:String, weight:Int) = {
    MultiplyConstant(LoadFile(path, re), weight)
  }

  def buildWoOp = {
    val rs = pairs.map { case (p, w) => buildScaleRasterOp(p, w) }.toArray
    Normalize(DivideConstant(Add(rs: _*), total), 1, 100)
  }

  def buildColorBreaksOp(r:Op[IntRaster]) = {
    FindColorBreaks(BuildArrayHistogram(r, 101), colors.length, colors)
  }

  def buildPNGOp(r:Op[IntRaster]) = {
    val i = Immutable(r)
    RenderPNG2(i, buildColorBreaksOp(i), 0, true)
  }

  val weight:Op[IntRaster] = buildWoOp
  val breaks:Op[ColorBreaks] = buildColorBreaksOp(weight)
  val render:Op[Array[Byte]] = buildPNGOp(weight)

  def run(reps:Int, server:Server) {
    var d:Array[Byte] = null
    for (i <- 0 until reps) d = server.run(render)
  }
}


class TrellisBenchmarks extends SimpleBenchmark {
  def buildPairs(ts:(String, Int)*) = ts.map { case (s, w) => (base + s + ".arg", w) }

  val base = "/home/eosheim/trellis/src/test/resources/sbn/SBN_"
  val pairs = buildPairs(("farm_mkt", 2), ("RR_stops_walk", 1), ("inc_percap", 5), ("street_den_1k", 2))
  val total = pairs.map(_._2).sum
  val colors = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)

  var server:Server = null

  var s64:WoBenchmark = null
  var s128:WoBenchmark = null
  var s256:WoBenchmark = null
  var s512:WoBenchmark = null
  var s1024:WoBenchmark = null
  var s2048:WoBenchmark = null
  var s4096:WoBenchmark = null
  var s8192:WoBenchmark = null

  var m100:MiniWoBenchmark = null
  var m1000:MiniWoBenchmark = null
  var m10000:MiniWoBenchmark = null

  var a64:MiniAddBenchmark = null
  var a128:MiniAddBenchmark = null
  var a256:MiniAddBenchmark = null
  var a512:MiniAddBenchmark = null
  var a1024:MiniAddBenchmark = null
  var a2048:MiniAddBenchmark = null
  var a4096:MiniAddBenchmark = null
  var a8192:MiniAddBenchmark = null

  override def setUp() {
    server = TestServer()

    val path = pairs(0)._1
    val extent = server.run(LoadRasterExtentFromFile(path)).extent

    m100 = MiniWoBenchmark(server, pairs(0), pairs(1), extent, 100)
    m1000 = MiniWoBenchmark(server, pairs(0), pairs(1), extent, 1000)
    m10000 = MiniWoBenchmark(server, pairs(0), pairs(1), extent, 10000)

    s64 = new WoBenchmark(64, extent, pairs, total, colors)
    s128 = new WoBenchmark(128, extent, pairs, total, colors)
    s256 = new WoBenchmark(256, extent, pairs, total, colors)
    s512 = new WoBenchmark(512, extent, pairs, total, colors)
    s1024 = new WoBenchmark(1024, extent, pairs, total, colors)
    s2048 = new WoBenchmark(2048, extent, pairs, total, colors)
    s4096 = new WoBenchmark(4096, extent, pairs, total, colors)
    s8192 = new WoBenchmark(8192, extent, pairs, total, colors)

    a64 = MiniAddBenchmark(server, path, extent, 64)
    a128 = MiniAddBenchmark(server, path, extent, 128)
    a256 = MiniAddBenchmark(server, path, extent, 256)
    a512 = MiniAddBenchmark(server, path, extent, 512)
    a1024 = MiniAddBenchmark(server, path, extent, 1024)
    a2048 = MiniAddBenchmark(server, path, extent, 2048)
    a4096 = MiniAddBenchmark(server, path, extent, 4096)
    a8192 = MiniAddBenchmark(server, path, extent, 8192)
  }

  def timeBasicWeightedOverlay_100(reps:Int) = m100.run(reps, server)
  def timeBasicWeightedOverlay_1000(reps:Int) = m1000.run(reps, server)
  def timeBasicWeightedOverlay_10000(reps:Int) = m10000.run(reps, server)

  def timeWeightedOverlayPNG_64(reps:Int) = s64.run(reps, server)
  def timeWeightedOverlayPNG_128(reps:Int) = s128.run(reps, server)
  def timeWeightedOverlayPNG_256(reps:Int) = s256.run(reps, server)
  def timeWeightedOverlayPNG_512(reps:Int) = s512.run(reps, server)
  def timeWeightedOverlayPNG_1024(reps:Int) = s1024.run(reps, server)
  def timeWeightedOverlayPNG_2048(reps:Int) = s2048.run(reps, server)
  // disabled
  //def timeWeightedOverlayPNG_4096(reps:Int) = s4096.run(reps, server)
  //def timeWeightedOverlayPNG_8192(reps:Int) = s8192.run(reps, server)

  def timeAddConstant_64(reps:Int) = a64.run(reps, server)
  def timeAddConstant_128(reps:Int) = a128.run(reps, server)
  def timeAddConstant_256(reps:Int) = a256.run(reps, server)
  def timeAddConstant_512(reps:Int) = a512.run(reps, server)
  def timeAddConstant_1024(reps:Int) = a1024.run(reps, server)
  def timeAddConstant_2048(reps:Int) = a2048.run(reps, server)
  def timeAddConstant_4086(reps:Int) = a4096.run(reps, server)
  def timeAddConstant_8192(reps:Int) = a8192.run(reps, server)
}

object TrellisBenchmarks {
  def main(args:Array[String]) {
    println("starting benchmarks...")
    Runner.main(classOf[TrellisBenchmarks], args:_*)
    println("completed benchmarks.")
  }
}
