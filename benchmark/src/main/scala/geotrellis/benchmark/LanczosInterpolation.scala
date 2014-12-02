package geotrellis.benchmark

import geotrellis.raster.interpolation._

import com.google.caliper.Param

object LanczosInterpolation extends BenchmarkRunner(classOf[LanczosInterpolation])

class LanczosInterpolation extends InterpolationBenchmark {

  def interp = Lanczos

}
