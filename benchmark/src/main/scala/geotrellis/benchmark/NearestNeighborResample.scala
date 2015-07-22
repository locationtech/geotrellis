package geotrellis.benchmark

import geotrellis.raster.resample._

import com.google.caliper.Param

object NearestNeighborResample
    extends BenchmarkRunner(classOf[NearestNeighborResample])

class NearestNeighborResample extends ResampleBenchmark {

  def resamp = NearestNeighbor

}
