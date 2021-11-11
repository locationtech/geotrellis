package geotrellis.raster

import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit
import scala.util.Random

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class GridBoxingBench {
  val size = 1024

  @Param(Array("short", "int"))
  var cellType: String = _

  var tile: MutableArrayTile = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    tile = cellType match {
      case "short" =>
        ShortArrayTile(
          1 to size * size map (_.toShort) toArray, size, size
        )
      case "int" =>
        IntArrayTile(
          1 to size * size toArray, size, size
        )
    }
  }

  var row: Int = _
  var col: Int = _
  var value: Int = _
  @Setup(Level.Invocation)
  def selectCell(): Unit = {
    row = Random.nextInt(size)
    col = Random.nextInt(size)
    value = Random.nextInt()
  }

  @Benchmark
  def setCell(): Tile = {
    tile.set(row, col, value)
    tile
  }
}
