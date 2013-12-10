package geotrellis.benchmark

import geotrellis._
import geotrellis.raster._
import geotrellis.raster.op.local._

import com.google.caliper.Param

import scala.util.Random

object MultiplyConstant extends BenchmarkRunner(classOf[MultiplyConstant])
class MultiplyConstant extends OperationBenchmark {
  //@Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  @Param(Array("2048"))
  var size:Int = 0

  var raster:Raster = null

  var mc:Op[Raster] = null
  var mcCustomWithInt:Op[Raster] = null
  var mcMapSugar:Op[Raster] = null
  var mcMapIfSetSugar:Op[Raster] = null
  var mcMapIfSetSugarWithLiteral:Op[Raster] = null
  var mcMapIfSet:Op[Raster] = null
  var mcWhileLoop:Op[Raster] = null

  override def setUp() {
    val len = size * size
    val re = RasterExtent(Extent(0, 0, size, size), 1.0, 1.0, size, size)
    raster = Raster(init(len)(Random.nextInt), re)

    mc = Multiply(raster, 2)
    mcCustomWithInt = MultiplyConstantCustomWithInt(raster, 2)
    mcMapIfSet = MultiplyConstantMapIfSet(raster, 2)
    mcMapSugar = MultiplyConstantMapSugar(raster, 2)
    mcMapIfSetSugar = MultiplyConstantMapIfSetSugar(raster, 2)
    mcMapIfSetSugarWithLiteral = MultiplyConstantMapIfSetSugarWithLiteral(raster, 2)
    mcWhileLoop = MultiplyConstantWhileLoop(raster, 2)
  }

  def timeRasterOperationUnary(reps:Int) = run(reps)(rasterOperationUnary)
  def rasterOperationUnary = get(mc)
  
  def timeRasterOperationCustomWithInt(reps:Int) = run(reps)(rasterOperationCustomWithInt)
  def rasterOperationCustomWithInt = get(mcCustomWithInt)
  
  def timeRasterOperationMapSugar(reps:Int) = run(reps)(rasterOperationMapSugar)
  def rasterOperationMapSugar = get(mcMapSugar)

  def timeRasterOperationMapIfSetSugar(reps:Int) = run(reps)(rasterOperationMapIfSetSugar)
  def rasterOperationMapIfSetSugar = get(mcMapIfSetSugar)

  def timeRasterOperationMapIfSetSugarWithLiteral(reps:Int) = run(reps)(rasterOperationMapIfSetSugarWithLiteral)
  def rasterOperationMapIfSetSugarWithLiteral = get(mcMapIfSetSugarWithLiteral)

  def timeRasterOperationMapIfSet(reps:Int) = run(reps)(rasterOperationMapIfSet)
  def rasterOperationMapIfSet = get(mcMapIfSet)

  def timeRasterOperationWhileLoop(reps:Int) = run(reps)(rasterOperationWhileLoop)
  def rasterOperationWhileLoop = get(mcWhileLoop)
}
