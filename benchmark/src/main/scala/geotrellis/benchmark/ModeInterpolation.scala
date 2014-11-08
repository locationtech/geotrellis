package geotrellis.benchmark

import geotrellis.raster.interpolation._

import com.google.caliper.Param

object ModeInterpolation extends BenchmarkRunner(classOf[ModeInterpolation])

class ModeInterpolation extends InterpolationBenchmark {

  def interp = Mode

}
