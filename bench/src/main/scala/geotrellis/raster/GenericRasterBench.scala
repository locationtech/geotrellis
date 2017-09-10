package geotrellis.raster

import geotrellis.bench.init
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.BenchmarkParams

import scala.util.Random


@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
@Threads(4)
class GenericRasterBench  {
  import GenericRasterBench._

  @Param(Array( "128", "256", "512", "1024", "2048", "4096", "8192"))
  var size: Int = _

  var tile: Tile = _
  var genericRaster: GRaster[Int] = _

  @Setup(Level.Trial)
  def setup(params: BenchmarkParams): Unit = {
    val len = size * size
    // Because we can end up allocating a lot of memory here,
    // we only initialize the data that is needed for the current benchmark.
    params.getBenchmark.split('.').last match {
      case "genericRasterMap" ⇒
        genericRaster = new GRaster(init(len)(Random.nextInt))
      case "rasterMap" ⇒
        tile = ArrayTile(init(len)(Random.nextInt), size, size)
      case _ ⇒ throw new MatchError("Have a new benchmark without initialization?")
    }
  }

  @Benchmark
  def genericRasterMap = {
    genericRaster.map { i => i * i }
  }

  @Benchmark
  def rasterMap = {
    tile.map { i => i * i }
  }

}

object GenericRasterBench {

  class GRaster[T](val array: Array[T]) {
    val size = array.length
    val newArr = array.clone
    def map(f: T=>T) = {
      var i = 0
      while(i < size) {
        newArr(i) = f(array(i))
        i += 1
      }
      new GRaster(newArr)
    }
  }

  class GRasterState {

  }
}
