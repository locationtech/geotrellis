package geotrellis.benchmark

import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.raster.op._

import com.google.caliper.Benchmark
import com.google.caliper.Param
import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark

import scala.math.{min, max}
import scala.util.Random


object CostDistanceOperationBenchmark extends BenchmarkRunner(classOf[CostDistanceOperationBenchmark])
class CostDistanceOperationBenchmark extends OperationBenchmark {
  val s = Server.empty("test")
  var r: Raster = _
  var p: List[(Int,Int)] = _

  @Param(Array("64", "128", "256", "512", "1024", "2048"))
  var rasterSize: Int = 0

  override def setUp() {
    val points = 3
    r = randomRasterN(rasterSize)
    p = randomPointList(rasterSize, points)
  }

  def timeCostDistance(reps:Int) = run(reps)(s.run(focal.CostDistance(r,p)))

  def randomRasterN(n: Int) = {
    val a = Array.ofDim[Int](n*n).map(a => Random.nextInt(255))
    val e = Extent(0,0,10*n,10*n)
    val re = RasterExtent(e, 10,10,n,n)
    val data = IntArrayRasterData(a, n, n)

    Raster(data, re)
  }

  def randomPointList(n: Int, nPoints: Int) = {
    var pts = Set[(Int,Int)]()
    while(pts.size < nPoints) {
      pts = pts + ((Random.nextInt(n), Random.nextInt(n)))
    }
    pts.toList.map { case (c,r) => (c,r) }
  }

}
