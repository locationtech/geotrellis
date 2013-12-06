package geotrellis.benchmark

import geotrellis._
import geotrellis.source._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object ConstantAdd extends BenchmarkRunner(classOf[ConstantAdd])
class ConstantAdd extends OperationBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "10000"))
  var size:Int = 0

  var source:RasterSource = null

  override def setUp() {
    source = 13 +: RasterSource(loadRaster("SBN_farm_mkt", size, size))
  }

  def timeConstantAdd(reps:Int) = run(reps)(constantAdd)
  def constantAdd = get(source)
}
