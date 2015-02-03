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


class ConstantTestFileValues(f: Float) extends TestFileValues {
  override def value(y: Int, x: Int): Float = f
}

class IncreasingTestFileValues(cols: Int, rows: Int) extends TestFileValues {
  override def value(y: Int, x: Int): Float = y * cols + x
}


class DecreasingTestFileValues(cols: Int, rows: Int) extends TestFileValues {
  override def value(y: Int, x: Int): Float = cols * rows - (y * cols + x) - 1
}


class EveryOtherUndefined(cols: Int) extends TestFileValues {
  override def value(y: Int, x: Int): Float =
    if ((y * cols + x)  % 2 == 0) Int.MinValue else 0
}

class EveryOther0Point99Else1Point01(cols: Int) extends TestFileValues {
  override def value(y: Int, x: Int): Float =
    if ((y * cols + x)  % 2 == 0) 0.99f else 1.01f
}


class EveryOther1ElseMinus1(cols: Int) extends TestFileValues {
  override def value(y: Int, x: Int): Float =
    if ((y * cols + x)  % 2 == 0) -1 else 1
}

class Mod(cols: Int, rows: Int, mod: Int) extends IncreasingTestFileValues(cols, rows) {
  override def value(y: Int, x: Int) = super.value(y, x) % mod

}
