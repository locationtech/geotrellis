package geotrellis.benchmark

/*
 * # Caliper API key for jmarcus@azavea.com
 * postUrl: http://microbenchmarks.appspot.com:80/run/
 * apiKey: 3226081d-9776-40f4-a2d7-a1dc99c948c6
*/

import geotrellis._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object NewAddOperations extends BenchmarkRunner(classOf[NewAddOperations])
class NewAddOperations extends OperationBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "8192", "10000"))
  var size:Int = 0
  
  var strictOld:Op[Raster] = null
  var strictNew:Op[Raster] = null
  var lazyNew:Op[Raster] = null

  override def setUp() {
    val r1:Raster = loadRaster("SBN_farm_mkt", size, size)
    val r2:Raster = loadRaster("SBN_RR_stops_walk", size, size)

    val l1:Raster = r1
    val l2:Raster = r2

    strictOld = AddOld(AddOld(r1, r2), AddOld(r1, r2))
    strictNew = Add(Add(r1, r2), Add(r1, r2))
    lazyNew = Add(Add(l1, l2), Add(l1, l2))
  }

  def timeStrictOld(reps:Int) = run(reps)(runStrictOld)
  def runStrictOld = get(strictOld)

  def timeStrictNew(reps:Int) = run(reps)(runStrictNew)
  def runStrictNew = get(strictNew)

  def timeLazyNew(reps:Int) = run(reps)(runLazyNew)
  def runLazyNew = get(lazyNew)
}
