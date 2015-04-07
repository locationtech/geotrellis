package geotrellis.spark.testfiles

import spire.syntax.cfor._

trait SpaceTimeTestFileValues {
  final def apply(cols: Int, rows: Int, z: Int): Array[Float] = {
    val arr = Array.ofDim[Float](cols * rows)

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        arr(row * cols + col) = value(row, col, z)
      }
    }

    arr
  }

  def value(y: Int, x: Int, z: Int): Float
}


class ConstantSpaceTimeTestFileValues(f: Float) extends SpaceTimeTestFileValues {
  def value(y: Int, x: Int, z: Int): Float = f
}

class CoordinateSpaceTimeTestFileValues extends SpaceTimeTestFileValues {
  def value(y: Int, x: Int, z: Int): Float =
    (x * 100) + (y * 10) + z
}
