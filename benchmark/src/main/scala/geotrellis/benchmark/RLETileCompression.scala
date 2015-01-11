package geotrellis.benchmark

import geotrellis.raster._

import com.google.caliper.Param

object RLETileCompression extends BenchmarkRunner(classOf[RLETileCompression])

class RLETileCompression extends TileCompressionBenchmark {

  def compression = RLE

}
