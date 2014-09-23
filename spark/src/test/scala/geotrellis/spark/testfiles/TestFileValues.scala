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

  def apply(f: Float): ConstantTestFileValues =
    new ConstantTestFileValues(f)

}

class ConstantTestFileValues(f: Float) extends TestFileValues {
  override def value(y: Int, x: Int): Float = f
}

object IncreasingTestFileValues {

  def apply(cols: Int, rows: Int, offset: Int = 0): IncreasingTestFileValues =
    new IncreasingTestFileValues(cols, rows, offset)

}

class IncreasingTestFileValues(
  cols: Int,
  rows: Int,
  offset: Int = 0) extends TestFileValues {
  override def value(y: Int, x: Int): Float = offset + y * cols + x
}

object DecreasingTestFileValues {

  def apply(cols: Int, rows: Int, offset: Int = 0): DecreasingTestFileValues =
    new DecreasingTestFileValues(cols, rows, offset)

}

class DecreasingTestFileValues(
  cols: Int,
  rows: Int,
  offset: Int = 0) extends TestFileValues {
  override def value(y: Int, x: Int): Float =
    cols * rows - (y * cols + x) - offset - 1
}

object EveryOtherUndefined {

  def apply(cols: Int) = new EveryOtherUndefined(cols)

}

class EveryOtherUndefined(cols: Int) extends TestFileValues {
  override def value(y: Int, x: Int): Float =
    if ((y * cols + x)  % 2 == 0) Int.MinValue else 0
}

object EveryOther0Point99Else1Point01 {

  def apply(cols: Int) =
    new EveryOther0Point99Else1Point01(cols)

}

class EveryOther0Point99Else1Point01(cols: Int) extends TestFileValues {
  override def value(y: Int, x: Int): Float =
    if ((y * cols + x)  % 2 == 0) 0.99f else 1.01f
}

object EveryOther1ElseMinus1 {

  def apply(cols: Int) =
    new EveryOther1ElseMinus1(cols)

}

class EveryOther1ElseMinus1(cols: Int) extends TestFileValues {
  override def value(y: Int, x: Int): Float =
    if ((y * cols + x)  % 2 == 0) -1 else 1
}
