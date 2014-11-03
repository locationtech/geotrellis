package geotrellis.benchmark

import geotrellis.raster.reproject._

import com.google.caliper.Param

object AverageInterpolation extends BenchmarkRunner(classOf[AverageInterpolation])

class AverageInterpolation extends InterpolationBenchmark {

  def interp = Average

}
