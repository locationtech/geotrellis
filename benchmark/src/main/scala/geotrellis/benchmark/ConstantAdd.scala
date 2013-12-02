package geotrellis.benchmark

import geotrellis._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object ConstantAdd extends BenchmarkRunner(classOf[ConstantAdd])
class ConstantAdd extends OperationBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "10000"))
  var size:Int = 0

  var op:Op[Raster] = null

  override def setUp() {
    val r:Raster = loadRaster("SBN_farm_mkt", size, size)
    op = Add(r, 13)
  }

  def timeConstantAdd(reps:Int) = run(reps)(constantAdd)
  def constantAdd = get(op)
}
