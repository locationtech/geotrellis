package geotrellis.benchmark

import geotrellis.raster.resample._

import com.google.caliper.Param

object LanczosResample extends BenchmarkRunner(classOf[LanczosResample])

class LanczosResample extends ResampleBenchmark {

  def resamp = Lanczos

}
