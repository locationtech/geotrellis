package geotrellis.benchmark

import geotrellis._
import geotrellis.source._
import geotrellis.raster.op.global
import geotrellis.raster.op.local._
import geotrellis.statistics.op.stat._
import geotrellis.io._
import geotrellis.render.op._
import geotrellis.render.png._

import com.google.caliper.Param

object RenderPngBenchmark extends BenchmarkRunner(classOf[RenderPngBenchmark])
class RenderPngBenchmark extends OperationBenchmark {
  val name = "SBN_farm_mkt"
  val colors = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)

  @Param(Array("256","512", "1024", "2048", "4096"))
  var size:Int = 0

  var op:Op[Png] = null
  var source:ValueSource[Png] = null

  override def setUp() {
    val re = getRasterExtent(name, size, size)
    val raster = get(io.LoadRaster(name,re))
    op =
      GetHistogram(raster).flatMap { h =>
        val breaksOp = GetColorBreaks(h, colors)
        RenderPng(raster, breaksOp, 0, h)
      }

    source = 
      RasterSource(name,re)
        .cached
        .renderPng(colors)
  }

  def timeRenderPngOp(reps:Int) = run(reps)(renderPngOp)
  def renderPngOp = get(op)

  def timeRenderPngSource(reps:Int) = run(reps)(renderPngSource)
  def renderPngSource = get(source)
}

object RenderPngWeightedOverlayBenchmark extends BenchmarkRunner(classOf[RenderPngWeightedOverlayBenchmark])
class RenderPngWeightedOverlayBenchmark extends OperationBenchmark {
  val n = 4
  val names = Array("SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k")
  val weights = Array(2, 1, 5, 2)
  val colors = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)

  @Param(Array("256","512", "1024", "2048", "4096"))
  var size:Int = 0

//  var op:Op[Raster] = null
  // var source:RasterSource = null
  //  var source2:RasterSource = null

  var op:Op[Png] = null
  var source:ValueSource[Png] = null
  var sourceSeq:ValueSource[Png] = null

  override def setUp() {
    val re = getRasterExtent(names(0), size, size)
    val total = weights.sum
    val rs = (0 until n).map(i => Multiply(LoadRaster(names(i), re), weights(i)))
    val weightedAdd = Add(rs: _*)
    val divided = Divide(weightedAdd, total)
//    op = divided
//    op = global.Rescale(divided, (1,100))
    val raster = get(global.Rescale(divided, (1, 100)))

    op =
      GetHistogram(raster) flatMap { h =>
        val breaksOp = GetColorBreaks(h, colors)
        RenderPng(raster, breaksOp, 0)
      }

    // source = 
    //   (0 until n).map(i => RasterSource(names(i),re) * weights(i))
    //              .reduce(_+_)
    //              .localDivide(total)
    //              .rescale(1,100)
    //              .renderPng(colors)



    // sourceSeq = 
    //   RasterSeqSource2((0 until n).map(i => RasterSource(names(i),re) * weights(i)))
    //              .localAdd
    //              .localDivide(total)
    //              .rescale(1,100)
    //              .renderPng(colors)
  }

  def timeWeightedOverlayOp(reps:Int) = run(reps)(weightedOverlayOp)
  def weightedOverlayOp = get(op)

  // def timeWeightedOverlaySource(reps:Int) = run(reps)(weightedOverlaySource)
  // def weightedOverlaySource = get(source)

  // def timeWeightedOverlaySourceSeq(reps:Int) = run(reps)(weightedOverlaySourceSeq)
  // def weightedOverlaySourceSeq = get(sourceSeq)
}


object RendererBenchmark extends BenchmarkRunner(classOf[RendererBenchmark])
class RendererBenchmark extends OperationBenchmark {
  val name = "SBN_farm_mkt"
  val names = Array("SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k")
  val weights = Array(2, 1, 5, 2)
  val colors = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)


  @Param(Array("512", "1024", "2048", "4096"))
  var size:Int = 0

  var renderer:Renderer = null
  var raster:Raster = null
  var rendered:Raster = null
  var data:Array[Int] = null

  override def setUp() {
    val re = getRasterExtent(names(0), size, size)
    val total = weights.sum
    val rs = (0 until names.length).map(i => Multiply(LoadRaster(names(i), re), weights(i)))
    val weightedAdd = Add(rs: _*)
    val divided = Divide(weightedAdd, total)
    val r = get(global.Rescale(divided, (1, 100)))
    val histogram = get(GetHistogram(r))
    val colorBreaks = get(GetColorBreaks(histogram,colors))
    val breaks = colorBreaks.limits
    val cs = colorBreaks.colors

    renderer = Renderer(breaks, cs, 0,histogram)
    raster = r
    data = r.asInstanceOf[ArrayRaster].data.toArray
    rendered = renderer.render(raster)
  }

  def timeMap(reps:Int) = run(reps)(runMap)
  def runMap = { 
    val len = data.length
    var i = 0
    while(i < len) { 
      val z = data(i)
      if(z == NODATA) 0 else 100
      i += 1
    }
  }
//    data.map { z => if(z == NODATA) 0 else 100 } }

  // def timeRenderer(reps:Int) = run(reps)(runRenderer)
  // def runRenderer = renderer.render(raster)

  // def timeEncoder(reps:Int) = run(reps)(runEncoder)
  // def runEncoder = {
  //   val r2 = renderer.render(raster)
  //   val bytes = new Encoder(renderer.settings).writeByteArray(r2)
  // }

  // def timeJustEncoder(reps:Int) = run(reps)(runJustEncoder)
  // def runJustEncoder = {
  //   val bytes = new Encoder(renderer.settings).writeByteArray(rendered)
  // }
}
