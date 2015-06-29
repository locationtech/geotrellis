package geotrellis.benchmark

import geotrellis.raster.resample._

import com.google.caliper.Param

object AverageResample extends BenchmarkRunner(classOf[AverageResample])

class AverageResample extends ResampleBenchmark {

  def resamp = Average

}
