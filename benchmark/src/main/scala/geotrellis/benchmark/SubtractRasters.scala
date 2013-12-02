package geotrellis.benchmark

import geotrellis._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object SubtractRasters extends BenchmarkRunner(classOf[SubtractRasters])
class SubtractRasters extends OperationBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  var size:Int = 0

  var op:Op[Raster] = null

  override def setUp() {
    val r:Raster = loadRaster("SBN_farm_mkt", size, size)
    val r1 = Multiply(r, 2)
    val r2 = Add(r, 2)
    op = Subtract(r1, r2)
  }

  def timeSubtractRasters(reps:Int) = run(reps)(subtractRasters)
  def subtractRasters = get(op)
}
