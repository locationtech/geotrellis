package geotrellis.benchmark

import geotrellis.raster._

import com.google.caliper.Param

object ZipTileCompression extends BenchmarkRunner(classOf[ZipTileCompression])

class ZipTileCompression extends TileCompressionBenchmark {

  def compression = Zip

}
