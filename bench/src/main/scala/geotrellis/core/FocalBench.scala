package geotrellis

import java.util.concurrent.TimeUnit

import geotrellis._
import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.{Sum, Square}
import org.openjdk.jmh.annotations._
import spire.std.any._

// --- //

/*
 jmh:run -t 1 -f 1 -wi 5 -i 5 .*FocalBench.*
 */

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
class FocalBench {

  var arr0: Array[Int] = _
  var tile: Tile = _

  @Setup
  def setup: Unit = {
    arr0 = Array.fill(65536)(1)
    tile = IntArrayTile(Array.fill(65536)(1), 256, 256)
  }

  @Benchmark
  def manualSum: Array[Int] = Focal[Array].fsum(arr0, Stencil.square(Boundary.Constant(0)))

  @Benchmark
  def original: Tile = Sum(tile, Square(1))

}
