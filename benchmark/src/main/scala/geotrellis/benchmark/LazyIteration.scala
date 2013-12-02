package geotrellis.benchmark

import geotrellis._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object LazyIteration extends BenchmarkRunner(classOf[LazyIteration])
class LazyIteration extends OperationBenchmark {
  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "8192", "10000"))
  var size:Int = 0
  
  @Param(Array("1", "2", "3", "4"))
  var iterations:Int = 0

  var simpleOp:Op[Raster] = null
  var mediumOp:Op[Raster] = null
  var complexOp:Op[Raster] = null

  override def setUp() {
    val r1:Raster = loadRaster("SBN_farm_mkt", size, size)
    val r2:Raster = loadRaster("SBN_RR_stops_walk", size, size)
    val r3:Raster = loadRaster("SBN_inc_percap", size, size)

    simpleOp = Add(r1, 6) 
    mediumOp = Add(Multiply(r1, 2), Multiply(r2, 3))
    complexOp = Add(Divide(Add(Multiply(r1, 2),
                               Multiply(r2, 3),
                               Multiply(r3, 4)), 9), mediumOp)
  }

  def timeSimpleOpLazyIteration(reps:Int) = run(reps)(simpleOpLazyIteration)
  def simpleOpLazyIteration = {
    val r = get(simpleOp)
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }

  def timeSimpleOpStrictIteration(reps:Int) = run(reps)(simpleOpStrictIteration)
  def simpleOpStrictIteration = {
    val r = get(simpleOp)
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }

  def timeMediumOpLazyIteration(reps:Int) = run(reps)(mediumOpLazyIteration)
  def mediumOpLazyIteration = {
    val r = get(mediumOp)
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }

  def timeMediumOpStrictIteration(reps:Int) = run(reps)(mediumOpStrictIteration)
  def mediumOpStrictIteration = {
    val r = get(mediumOp)
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }

  def timeComplexOpLazyIteration(reps:Int) = run(reps)(complexOpLazyIteration)
  def complexOpLazyIteration = {
    val r = get(complexOp)
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }

  def timeComplexOpStrictIteration(reps:Int) = run(reps)(complexOpStrictIteration)
  def complexOpStrictIteration = {
    val r = get(complexOp)
    var t = 0
    for (i <- 0 until iterations) {
      r.foreach(z => t = t + z)
    }
    t
  }
}
