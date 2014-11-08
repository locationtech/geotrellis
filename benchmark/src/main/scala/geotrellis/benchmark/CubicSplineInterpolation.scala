package geotrellis.benchmark

import geotrellis.raster.interpolation._

import com.google.caliper.Param

object CubicSplineInterpolation
    extends BenchmarkRunner(classOf[CubicSplineInterpolation])

class CubicSplineInterpolation extends InterpolationBenchmark {

  def interp = CubicSpline

}
