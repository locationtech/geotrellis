package geotrellis.benchmark

import geotrellis.raster.resample._

import com.google.caliper.Param

object BilinearResample extends BenchmarkRunner(classOf[BilinearResample])

class BilinearResample extends ResampleBenchmark {

  def resamp = Bilinear

}
