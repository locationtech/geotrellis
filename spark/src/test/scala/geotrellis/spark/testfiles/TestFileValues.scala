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

  def value(y: Int, x: Int): Float
}

object ConstantTestFileValues {

  def apply(f: Float)(offset: Int = 0): ConstantTestFileValues =
    new ConstantTestFileValues(f, offset)

}

class ConstantTestFileValues(f: Float, offset: Int = 0) extends TestFileValues {
  override def value(y: Int, x: Int): Float = f + offset
}

object IncreasingTestFileValues {

  def apply(cols: Int, rows: Int)(offset: Int = 0): IncreasingTestFileValues =
    new IncreasingTestFileValues(cols, rows, offset)

}

class IncreasingTestFileValues(
  cols: Int,
  rows: Int,
  offset: Int = 0) extends TestFileValues {
  override def value(y: Int, x: Int): Float = y * cols + x + offset
}

object DecreasingTestFileValues {

  def apply(cols: Int, rows: Int)(offset: Int = 0): DecreasingTestFileValues =
    new DecreasingTestFileValues(cols, rows, offset)

}

class DecreasingTestFileValues(
  cols: Int,
  rows: Int,
  offset: Int = 0) extends TestFileValues {
  override def value(y: Int, x: Int): Float =
    cols * rows - (y * cols + x) - 1 + offset
}

object EveryOtherUndefined {

  def apply(cols: Int)(other: Int = 0) = new EveryOtherUndefined(cols, other)

}

class EveryOtherUndefined(cols: Int, other: Int = 0) extends TestFileValues {
  override def value(y: Int, x: Int): Float =
    if ((y * cols + x)  % 2 == 0) Int.MinValue else other
}

object EveryOther0Point99Else1Point01 {

  def apply(cols: Int)(offset: Int = 0) =
    new EveryOther0Point99Else1Point01(cols, offset)

}

class EveryOther0Point99Else1Point01(
  cols: Int,
  offset: Int = 0) extends TestFileValues {
  override def value(y: Int, x: Int): Float =
    (if ((y * cols + x)  % 2 == 0) 0.99f else 1.01f) + offset
}

object EveryOther1ElseMinus1 {

  def apply(cols: Int)(offset: Int = 0) = new EveryOther1ElseMinus1(cols, offset)

}

class EveryOther1ElseMinus1(cols: Int, offset: Int = 0) extends TestFileValues {
  override def value(y: Int, x: Int): Float =
    (if ((y * cols + x)  % 2 == 0) -1 else 1) + offset
}
