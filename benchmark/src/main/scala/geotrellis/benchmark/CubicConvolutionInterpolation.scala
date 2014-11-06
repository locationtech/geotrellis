package geotrellis.benchmark

import geotrellis.raster.interpolation._

import com.google.caliper.Param

object CubicConvolutionInterpolation
    extends BenchmarkRunner(classOf[CubicConvolutionInterpolation])

class CubicConvolutionInterpolation extends InterpolationBenchmark {

  def interp = CubicConvolution

}
