package geotrellis.benchmark

import geotrellis._
import geotrellis.source._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object ConstantAdd extends BenchmarkRunner(classOf[ConstantAdd])
class ConstantAdd extends OperationBenchmark {
  @Param(Array("bit","byte","short","int","float","double"))
  var rasterType = ""

  val layers = 
    Map(
      ("bit","wm_DevelopedLand"),
      ("byte", "SBN_car_share"),
      ("short","travelshed-int16"),
      ("int","travelshed-int32"),
      ("float","aspect"), 
      ("double","aspect-double")
    )

  @Param(Array("128", "256", "512"))
  var size:Int = 0

  var op:Op[Raster] = null
  var source:RasterSource = null

  override def setUp() {
    val id = layers(rasterType)
    op = Add(loadRaster(id,size,size),13)
    source = 13 +: RasterSource(loadRaster(id, size, size))
  }

  def timeConstantAddOp(reps:Int) = run(reps)(constantAddOp)
  def constantAddOp = get(source)

  def timeConstantAddSource(reps:Int) = run(reps)(constantAddSource)
  def constantAddSource = get(source)
}
