package geotrellis.benchmark

import geotrellis._

import com.google.caliper.Param

object RasterForeach extends BenchmarkRunner(classOf[RasterForeach])
class RasterForeach extends OperationBenchmark {
  //@Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "8192", "10000"))
  @Param(Array("64", "128", "256", "512", "1024"))
  var size:Int = 0
  
  var r:Raster = null

  override def setUp() {
    r = get(loadRaster("SBN_farm_mkt", size, size))
  }

  def timeRasterForeach(reps:Int) = run(reps)(rasterForeach)
  def rasterForeach = {
    var t = 0
    r.foreach(z => t += z)
    t
  }

  def timeRasterWhile(reps:Int) = run(reps)(rasterWhile)
  def rasterWhile = {
    var t = 0
    var i = 0
    val d = r.toArray
    val len = r.cols*r.rows
    while (i < len) {
      t += d(i)
      i += 1
    }
    t
  }
}
