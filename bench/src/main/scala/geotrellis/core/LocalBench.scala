package geotrellis

import java.util.concurrent.TimeUnit

import geotrellis._
import org.openjdk.jmh.annotations._
import spire.std.any._

// --- //

/*
 jmh:run -t 1 -f 1 -wi 5 -i 5 .*LocalBench.*
 */

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
class LocalBench {

  var arr0: Array[Int] = _
  var arr1: Array[Int] = _

  @Setup
  def setup: Unit = {
    arr0 = Array.range(1, 10000)
    arr1 = Array.range(1, 10000)
  }

  def sum(a: Array[Int], b: Array[Int]): Array[Int] = a + b

  def zip(a: Array[Int], b: Array[Int]): Array[Int] = a.zipWith(b, { _ + _ })

  def manualZip(a: Array[Int], b: Array[Int]): Array[Int] = Local[Array].zipWith(a, b, { _ + _ })

  def manualSum(a: Array[Int], b: Array[Int]): Array[Int] = Local[Array].+(a, b)

  def withWhile(a: Array[Int], b: Array[Int]): Array[Int] = {
    if (a.isEmpty) a else {
      val len: Int = a.size.min(b.size)
      val res: Array[Int] = if (a.size < b.size) a.clone else b.clone
      var i: Int = 0

      while(i < len) {
        res(i) = a(i) + b(i)

        i += 1
      }

      res
    }
  }

  @Benchmark
  def sumB: Array[Int] = sum(arr0, arr1)

  @Benchmark
  def zipB: Array[Int] = zip(arr0, arr1)

  @Benchmark
  def manualZipB: Array[Int] = manualZip(arr0, arr1)

  @Benchmark
  def manualSumB: Array[Int] = manualSum(arr0, arr1)

  @Benchmark
  def whileB: Array[Int] = withWhile(arr0, arr1)
}
