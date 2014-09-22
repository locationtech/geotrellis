package geotrellis.spark.testfiles

import spire.syntax.cfor._

trait TestFileValues {
  final def apply(cols: Int, rows: Int): Array[Float] = {
    val arr = Array.ofDim[Float](cols * rows)

    cfor(0)(_ < rows, _ + 1) { i =>
      cfor(0)(_ < cols, _ + 1) { j =>
        arr(i * cols + j) = value(i, j)
      }
    }

    arr
  }

  def value(y: Int, x: Int): Int
}

object ConstantTestFileValues {

  def apply(v: Int): ConstantTestFileValues =
    new ConstantTestFileValues(v)

}

class ConstantTestFileValues(v: Int) extends TestFileValues {
  override def value(y: Int, x: Int): Int = v
}

object IncreasingTestFileValues {

  def apply(cols: Int, rows: Int, offset: Int = 0): IncreasingTestFileValues =
    new IncreasingTestFileValues(cols, rows, offset)

}

class IncreasingTestFileValues(
  cols: Int,
  rows: Int,
  offset: Int = 0) extends TestFileValues {
  override def value(y: Int, x: Int): Int = offset + y * cols + x
}

object DecreasingTestFileValues {

  def apply(cols: Int, rows: Int, offset: Int = 0): DecreasingTestFileValues =
    new DecreasingTestFileValues(cols, rows, offset)

}

class DecreasingTestFileValues(
  cols: Int,
  rows: Int,
  offset: Int = 0) extends TestFileValues {
  override def value(y: Int, x: Int): Int =
    cols * rows - (y * cols + x) - offset - 1
}
