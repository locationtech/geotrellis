package geotrellis.benchmark

import geotrellis._
import geotrellis.source._
import geotrellis.raster.op._
import geotrellis.raster.op.local._
import geotrellis.statistics.op.stat._
import geotrellis.io._
import geotrellis.render.op._

import com.google.caliper.Param

object WeightedOverlay extends BenchmarkRunner(classOf[WeightedOverlay])
class WeightedOverlay extends OperationBenchmark {
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
    op = 
      global.Rescale(divided, (1, 100)) flatMap { r =>
        GetHistogram(r) flatMap { h =>
          val breaksOp = GetColorBreaks(h, colors)
          RenderPng(r, breaksOp, 0)
        }
      }

    source = 
      (0 until n).map(i => RasterSource(names(i),re) * weights(i))
                 .reduce(_+_)
                 .localDivide(total)
                 .rescale(1,100)
                 .renderPng(colors)



    sourceSeq = 
      RasterSeqSource2((0 until n).map(i => RasterSource(names(i),re) * weights(i)))
                 .localAdd
                 .localDivide(total)
                 .rescale(1,100)
                 .renderPng(colors)
  }

  def timeWeightedOverlayOp(reps:Int) = run(reps)(weightedOverlayOp)
  def weightedOverlayOp = get(op)

  def timeWeightedOverlaySource(reps:Int) = run(reps)(weightedOverlaySource)
  def weightedOverlaySource = get(source)

  def timeWeightedOverlaySourceSeq(reps:Int) = run(reps)(weightedOverlaySourceSeq)
  def weightedOverlaySourceSeq = get(sourceSeq)
}
