package geotrellis.benchmark

import geotrellis._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object AddRasters extends BenchmarkRunner(classOf[AddRasters])
class AddRasters extends OperationBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  var size:Int = 0

  var op:Op[Raster] = null

  override def setUp() {
    val r:Raster = loadRaster("SBN_farm_mkt", size, size)
    val r1 = Add(r, 1)
    val r2 = Add(r, 2)
    val r3 = Add(r, 3)
    val r4 = Add(r, 4)
    val r5 = Add(r, 5)
    op = Add(r1, r2, r3, r4, r5)
  }

  def timeAddRasters(reps:Int) = run(reps)(addRasters)
  def addRasters = get(op)
}
