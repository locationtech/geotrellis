package geotrellis.benchmark

import geotrellis.raster.resample._

import com.google.caliper.Param

object ModeResample extends BenchmarkRunner(classOf[ModeResample])

class ModeResample extends ResampleBenchmark {

  def resamp = Mode

}
