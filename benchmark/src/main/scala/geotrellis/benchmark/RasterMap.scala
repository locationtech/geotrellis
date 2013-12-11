package geotrellis.benchmark

import geotrellis._
import geotrellis.source._
import geotrellis.raster._
import geotrellis.raster.op.local._

import com.google.caliper.Param

import scala.util.Random
import scala.annotation.tailrec

object RasterMap extends BenchmarkRunner(classOf[RasterMap])
class RasterMap extends OperationBenchmark {
  @Param(Array("256", "512", "1024", "2048", "4096"))
  var size:Int = 0

  var raster:Raster = null

  var computedRaster:Raster = null
  val names = Array("SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k")
  val weights = Array(2, 1, 5, 2)


  override def setUp() {
    val len = size * size
    val re = RasterExtent(Extent(0, 0, size, size), 1.0, 1.0, size, size)
    raster = Raster(init(len)(Random.nextInt), re)
    computedRaster =
      (0 until names.length)
        .map(i => RasterSource(names(i),re) * weights(i))
        .reduce(_+_)
        .localDivide(weights.sum)
        .rescale(1,100)
        .get
  }

  def timeRasterMap(reps:Int) = run(reps)(rasterMap)
  def rasterMap = raster.map(z => if (isData(z)) z * 2 else NODATA)

  def timeRasterMapIfSet(reps:Int) = run(reps)(rasterMapIfSet)
  def rasterMapIfSet = raster.mapIfSet(z => z * 2)

  def timeRasterMapComputed(reps:Int) = run(reps)(rasterMapComputed)
  def rasterMapComputed = computedRaster.map(z => if (isData(z)) z * 2 else NODATA)

  def timeRasterMapIfSetComputed(reps:Int) = run(reps)(rasterMapIfSetComputed)
  def rasterMapIfSetComputed = computedRaster.mapIfSet(z => z * 2)
}
