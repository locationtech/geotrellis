package geotrellis.feature.benchmark

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
trait FeatureBenchmark extends SimpleBenchmark {
  /**
   * Sugar to run 'f' for 'reps' number of times.
   */
  def run(reps:Int)(f: => Unit) = {
    var i = 0
    while (i < reps) { f; i += 1 }
  }
}
