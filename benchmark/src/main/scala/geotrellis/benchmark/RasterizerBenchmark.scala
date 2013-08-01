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


object RasterizerBenchmark extends BenchmarkRunner(classOf[RasterizerBenchmark])
class RasterizerBenchmark extends OperationBenchmark {
  var r: Raster = _
  var re: RasterExtent = _
  var data: IntArrayRasterData = _
  var poly: feature.Polygon[Int] = _

  @Param(Array("512","1024","2048","4096","8192"))
  var rasterSize: Int = 0

  override def setUp() {
    r = randomRasterN(rasterSize)
    // rasters go from 0,0 to 10n,10n so we'll stick
    // a triangle in here
    poly = feature.Polygon(Seq((0,0),(10*rasterSize,0),(10*rasterSize/2, 10*rasterSize),(0,0)), 1)
  }

  def rasterize() {
    feature.rasterize.Rasterizer.foreachCellByFeature(poly, re)(
      new feature.rasterize.Callback[feature.Polygon,Int] {
        def apply(col: Int, row: Int, g: feature.Polygon[Int]) {
          data.set(col,row,4)
        }
      })
  }

  def rasterizeUsingValue() {
    feature.rasterize.Rasterizer.foreachCellByFeature(poly, re)(
      new feature.rasterize.Callback[feature.Polygon,Int] {
        def apply(col: Int, row: Int, g: feature.Polygon[Int]) {
          data.set(col,row,g.data)
        }
      })
  }


  def timeRasterizer(reps:Int) = run(reps)(rasterize())
  def timeRasterizerUsingValue(reps:Int) = run(reps)(rasterize())

  def randomRasterN(n: Int) = {
    val a = Array.ofDim[Int](n*n).map(a => Random.nextInt(255))
    val e = Extent(0,0,10*n,10*n)
    re = RasterExtent(e, 10,10,n,n)
    data = IntArrayRasterData(a, n, n)

    new Raster(data, re)
  }
}
