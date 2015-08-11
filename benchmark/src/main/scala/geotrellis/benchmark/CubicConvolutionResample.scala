package geotrellis.benchmark

import geotrellis.raster.resample._

import com.google.caliper.Param

object CubicConvolutionResample
    extends BenchmarkRunner(classOf[CubicConvolutionResample])

class CubicConvolutionResample extends ResampleBenchmark {

  def resamp = CubicConvolution

}
