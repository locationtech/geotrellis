package geotrellis.spark

import org.scalatest._

trait ArrayMatchers extends Matchers {

  val Eps = 1e-3

  def compareDoubleArrays(res: Array[Double], expected: Array[Double]) =
    for (i <- 0 until res.size)
      res(i) should be(expected(i) +- Eps)

}
