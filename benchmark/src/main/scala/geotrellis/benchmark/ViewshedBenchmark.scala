package geotrellis.benchmark

/**
 * Created by jchien on 4/24/14.
 */
import geotrellis._
import geotrellis.raster._

import com.google.caliper.Param
import scala.util.Random
import geotrellis.raster.op.global.Viewshed

/*
 * Current times
 * [info] CornerRequiredHeight 1363 ==============================
 * [info] CenterRequiredHeight  680 ==============
 */
object ViewshedBenchmark extends BenchmarkRunner(classOf[ViewshedBenchmark])
class  ViewshedBenchmark extends OperationBenchmark {
  var r: Raster = _
  var re: RasterExtent = _
  var data: IntArrayRasterData = _

  //@Param(Array("512","1024","2048","4096","8192"))
  var size:Int = 256

  override def setUp() {
    r = randomRasterN(size)
  }

  def cornerRequiredHeight() {
    Viewshed.computeHeightRequired(0, 0, r)
  }
  def centerRequiredHeight() {
    Viewshed.computeHeightRequired(size/2, size/2, r)
  }

  def randomRasterN(n: Int):Raster = {
    val a = Array.ofDim[Int](n*n).map(a => Random.nextInt(255))
    val e = Extent(0,0,10*n,10*n)
    re = RasterExtent(e, 10,10,n,n)
    data = IntArrayRasterData(a, n, n)

    Raster(data, re)
  }

  def timeCornerRequiredHeight(reps:Int) = run(reps)(cornerRequiredHeight)
  def timeCenterRequiredHeight(reps:Int) = run(reps)(centerRequiredHeight)
}
