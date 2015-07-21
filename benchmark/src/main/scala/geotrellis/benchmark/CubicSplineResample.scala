package geotrellis.benchmark

import geotrellis.raster.resample._

import com.google.caliper.Param

object CubicSplineResample
    extends BenchmarkRunner(classOf[CubicSplineResample])

class CubicSplineResample extends ResampleBenchmark {

  def resamp = CubicSpline

}
