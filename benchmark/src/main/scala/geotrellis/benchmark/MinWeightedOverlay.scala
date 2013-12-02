package geotrellis.benchmark

import geotrellis._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object MiniWeightedOverlay extends BenchmarkRunner(classOf[MiniWeightedOverlay])
class MiniWeightedOverlay extends OperationBenchmark {
  //@Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "8192", "10000"))
  @Param(Array("100", "1000", "10000"))
  var size:Int = 0
  
  var op:Op[Raster] = null

  var strictOp:Op[Raster] = null
  var lazyOp:Op[Raster] = null

  override def setUp() {
    val r1:Raster = loadRaster("SBN_farm_mkt", size, size)
    val r2:Raster = loadRaster("SBN_RR_stops_walk", size, size)

    op = AddOld(Multiply(r1, 5), Multiply(r2, 2))

    strictOp = Add(MultiplyConstantMapSugar(r1, 5), MultiplyConstantMapSugar(r2, 2))
    lazyOp = Add(MultiplyConstantMapSugar(r1, 5), MultiplyConstantMapSugar(r2, 2))
  }

  def timeMiniWeightedOverlay(reps:Int) = run(reps)(miniWeightedOverlay)
  def miniWeightedOverlay = get(op)

  def timeMiniWeightedOverlayStrict(reps:Int) = run(reps)(miniWeightedOverlayStrict)
  def miniWeightedOverlayStrict = get(strictOp)

  def timeMiniWeightedOverlayLazy(reps:Int) = run(reps)(miniWeightedOverlayLazy)
  def miniWeightedOverlayLazy = get(lazyOp)
}
