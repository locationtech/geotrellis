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
  var re: RasterExtent = _
  var data: IntArrayRasterData = _

  //@Param(Array("512","1024","2048","4096","8192"))
  var size:Int = 256

  override def setUp() {
    r = randomRasterN(size)
  }

  def cornerViewable() {
    ApproxViewshed.approxComputeViewable(0, 0, r)
  }
  def centerViewable () {
    ApproxViewshed.approxComputeViewable(size/2, size/2, r)
  }

  def randomRasterN(n: Int):Raster = {
    val a = Array.ofDim[Int](n*n).map(a => Random.nextInt(255))
    val e = Extent(0,0,10*n,10*n)
    re = RasterExtent(e, 10,10,n,n)
    data = IntArrayRasterData(a, n, n)

    Raster(data, re)
  }

  def timeCornerViewable(reps:Int) = run(reps)(cornerViewable)
  def timeCenterViewable(reps:Int) = run(reps)(centerViewable)
}

