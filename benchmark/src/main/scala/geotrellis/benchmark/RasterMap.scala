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
//  @Param(Array("512"))
  var size:Int = 0

  var raster:Raster = null
  var doubleRaster:Raster = null

  var computedRaster:Raster = null
  val names = Array("SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k")
  val weights = Array(2, 1, 5, 2)

  var array:Array[Byte] = null
  var length:Int = 0

  var rasterData:ByteArrayRasterData = null

  def thisMap(r:Raster, f:Int => Int) = r map { i =>
      if(isNoData(i)) i
      else f(i)
    }

  override def setUp() {
    val len = size * size
    val re = RasterExtent(Extent(0, 0, size, size), 1.0, 1.0, size, size)
    raster = Raster(init(len)(Random.nextInt), re)

    doubleRaster = Raster(init(len)(Random.nextDouble), re)

    val re2 = getRasterExtent(names(0), size, size)

    computedRaster =
      (0 until names.length)
        .map(i => RasterSource(names(i),re2) * weights(i))
        .reduce(_+_)
        .localDivide(weights.sum)
        .rescale(1,100)
        .get

    rasterData = computedRaster.asInstanceOf[ArrayRaster].data.asInstanceOf[ByteArrayRasterData]
    array = rasterData.array
    length = array.length
  }

  def timeRasterMap(reps:Int) = run(reps)(rasterMap)
  def rasterMap = raster map { i => i * 2 }

  def timeRasterMapIfSet(reps:Int) = run(reps)(rasterMapIfSet)
  def rasterMapIfSet = raster.mapIfSet(z => z * 2)

  def timeRasterMapDouble(reps:Int) = run(reps)(rasterMapDouble)
  def rasterMapDouble = raster.mapDouble(z => if (isData(z)) z * 2.0 else Double.NaN)

  def timeRasterMapIfSetDouble(reps:Int) = run(reps)(rasterMapIfSetDouble)
  def rasterMapIfSetDouble = doubleRaster.mapIfSetDouble(z => z * 2 )

  def timeRasterMapComputed(reps:Int) = run(reps)(rasterMapComputed)
  def rasterMapComputed = computedRaster.map(z => if (isData(z)) z * 2 else NODATA)

  def timeRasterMapIfSetComputed(reps:Int) = run(reps)(rasterMapIfSetComputed)
  def rasterMapIfSetComputed = computedRaster.mapIfSet(z => z * 2)

}
