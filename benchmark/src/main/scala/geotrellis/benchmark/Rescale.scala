package geotrellis.benchmark

import geotrellis._
import geotrellis.source._
import geotrellis.raster.op.global
import geotrellis.io._

import com.google.caliper.Param

object RescaleBenchmark extends BenchmarkRunner(classOf[RescaleBenchmark])
class RescaleBenchmark extends OperationBenchmark {
  val n = 10
  val name = "SBN_farm_mkt"

  @Param(Array("256","512", "1024", "2048", "4096", "8192"))
  var size:Int = 0

  var op:Op[Raster] = null
  var source:RasterSource = null
  override def setUp() {
    val re = getRasterExtent(name, size, size)
    val raster = get(LoadRaster(name,re))
    op = global.Rescale(raster, (0,100))

    source = 
      RasterSource(name,re)
        .cached
        .rescale(0,100)
  }

  def timeRescaleOp(reps:Int) = run(reps)(rescaleOp)
  def rescaleOp = get(op)

  def timeRescaleSource(reps:Int) = run(reps)(rescaleSource)
  def rescaleSource = get(source)
}
