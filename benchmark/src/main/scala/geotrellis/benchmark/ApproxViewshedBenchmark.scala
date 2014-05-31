package geotrellis.benchmark

import geotrellis.{Raster, RasterExtent}
import geotrellis.feature._
import geotrellis.raster._

import com.google.caliper.Param
import scala.util.Random
import geotrellis.raster.op.global.ApproxViewshed

/*
 * Current times for 256x256 (ms)
 * [info] CornerViewable 19.53 ==============================
 * [info] CenterViewable  9.19 ==============
 */
object ApproxViewshedBenchmark extends BenchmarkRunner(classOf[ApproxViewshedBenchmark])
class  ApproxViewshedBenchmark extends OperationBenchmark {
  var r: Raster = _
  var data: IntArrayRasterData = _

  //@Param(Array("512","1024","2048","4096","8192"))
  var size:Int = 256

  override def setUp() {
    r = {
      val a = Array.ofDim[Int](size * size).map(a => Random.nextInt(255))
      val e = Extent(0, 0, 10 * size, 10 * size)
      data = IntArrayRasterData(a, size, size)

      Raster(data, size, size)
    }
  }

  def timeCornerViewable(reps:Int) = run(reps)(cornerViewable)
  def cornerViewable() {
    ApproxViewshed(r, 0, 0)
  }

  def timeCenterViewable(reps:Int) = run(reps)(centerViewable)
  def centerViewable () {
    ApproxViewshed(r, size/2, size/2)
  }

}

