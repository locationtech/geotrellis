package geotrellis

import java.util.concurrent.TimeUnit

import cats.Monoid
import cats.implicits._
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
  var arrC: Array[Pair] = _
  var tile: Tile = _

  @Setup
  def setup: Unit = {
    arr0 = Array.fill(65536)(1)
    arrC = Array.fill(65536)(Pair(1, 1))
    tile = IntArrayTile(Array.fill(65536)(1), 256, 256)
  }

  @Benchmark
  def cloneArray: Array[Int] = arr0.clone

  @Benchmark
  def manualSum: Array[Int] = Focal[Array].fsum(arr0, Stencil.square(Boundary.Constant(0)))

  @Benchmark
  def fold: Array[Int] = Focal[Array].fold(arr0, Stencil.square(Boundary.Constant(0)))

  @Benchmark
  def foldClass: Array[Pair] = Focal[Array].fold(arrC, Stencil.square(Boundary.Constant(Monoid[Pair].empty)))

  @Benchmark
  def focalClass: Array[Pair] = Focal[Array].focal(
    arrC,
    Stencil.square(Boundary.Constant(Pair(0,0))),
    { n => n.foldLeft(Pair(0,0)) { _ |+| _ } }
  )

  @Benchmark
  def original: Tile = Sum(tile, Square(1))

}

case class Pair(a: Int, b: Int)

object Pair {
  implicit val pairMonoid: Monoid[Pair] = new Monoid[Pair] {
    def empty: Pair = Pair(0, 0)

    def combine(x: Pair, y: Pair): Pair = Pair(x.a + y.a, x.b + y.b)
  }
}
