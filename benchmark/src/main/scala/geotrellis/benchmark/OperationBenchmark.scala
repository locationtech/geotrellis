package geotrellis.benchmark

import geotrellis._
import geotrellis.process._

import com.google.caliper.Benchmark
import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark

/**
 * Extend this to create a main object which will run 'cls' (a benchmark).
 */
abstract class BenchmarkRunner(cls:java.lang.Class[_ <: Benchmark]) {
  def main(args:Array[String]): Unit = Runner.main(cls, args:_*)
}

/**
 * Extend this to create an actual benchmarking class.
 */
trait OperationBenchmark extends SimpleBenchmark {
  def getRasterExtent(name:String, w:Int, h:Int) =
    RasterExtent(GeoTrellis.get(io.LoadRasterExtent(name)).extent,w,h)

  /**
   * Loads a given raster with a particular height/width.
   */
  def loadRaster(name:String, w:Int, h:Int) = {
    GeoTrellis.get(io.LoadRaster(name, getRasterExtent(name, w, h)))
  }

  def get[T](op:Op[T]) = GeoTrellis.get(op)

  /**
   * Load a server with the GeoTrellis benchmarking catalog.
   */
//  def initServer():Server = GeoTrellis.server//BenchmarkServer.server

  /**
   * Sugar for building arrays using a per-cell init function.
   */
  def init[A:Manifest](size:Int)(init: => A) = {
    val data = Array.ofDim[A](size)
    for (i <- 0 until size) data(i) = init
    data
  }

  /**
   * Sugar to run 'f' for 'reps' number of times.
   */
  def run(reps:Int)(f: => Unit) = {
    var i = 0
    while (i < reps) { f; i += 1 }
  }
}
