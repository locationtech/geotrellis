package geotrellis.benchmark

import geotrellis.raster.interpolation._

import com.google.caliper.Param

object BilinearInterpolation extends BenchmarkRunner(classOf[BilinearInterpolation])

class BilinearInterpolation extends InterpolationBenchmark {

  def interp = Bilinear

}
