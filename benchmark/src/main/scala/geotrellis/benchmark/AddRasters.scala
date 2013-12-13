package geotrellis.benchmark

import geotrellis._
import geotrellis.source._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object AddRasters extends BenchmarkRunner(classOf[AddRasters])
class AddRasters extends OperationBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  var size:Int = 0

  var source:RasterSource = null

  override def setUp() {
    val r = RasterSource(loadRaster("SBN_farm_mkt", size, size))
    val r1 = r+1
    val r2 = r+2
    val r3 = r+3
    val r4 = r+4
    val r5 = r+5
    source = (r1+r2+r3+r4+r5)
  }

  def timeAddRasters(reps:Int) = run(reps)(addRasters)
  def addRasters = get(source)
}

