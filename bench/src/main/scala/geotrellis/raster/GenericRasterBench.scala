package geotrellis.raster

import geotrellis.bench.init
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.mapalgebra.local._
import org.openjdk.jmh.annotations._

import scala.util.Random



@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
class GenericRasterBench  {
  import GenericRasterBench._

  @Param(Array("128", "256", "512", "1024", "2048", "4096", "8192"))
  var size: Int = _

  var tile: Tile = _
  var genericRaster: GRaster[Int] = _

  @Setup(Level.Invocation)
  def setup(): Unit = {
    val len = size * size
    tile = ArrayTile(init(len)(Random.nextInt), size, size)
    genericRaster = new GRaster(init(len)(Random.nextInt))
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
}
