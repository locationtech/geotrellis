package geotrellis.benchmark

import geotrellis._
import geotrellis.operation._
import geotrellis.process._

/**
 * Here is a MultiplyConstant implementation in terms of a re-imaginined
 * UnaryLocal/WithInt implementation. This implementation is very similar to
 * the implementation of MultiplyConstantMapIfSet.
 *
 * NOTE: I'm worried that multiple subclasses of CustomWithInt might trigger
 * de-inlining, which could hurt performance in practice. But at this point
 * that concern is mostly superstitious.
 */
abstract class CustomWithInt(r:Op[Raster], c:Op[Int]) extends Op[Raster] {
  final def _run(context:Context) = runAsync(r :: c :: Nil)

  def handleCell(z:Int, n:Int): Int

  final val nextSteps:Steps = {
    case (raster:Raster) :: (n:Int) :: Nil => Result(raster.mapIfSet(z => handleCell(z, n)))
  }
}
case class MultiplyConstantCustomWithInt(r:Op[Raster], c:Op[Int]) extends CustomWithInt(r, c) {
  @inline final def handleCell(z:Int, n:Int) = z * n
}

/**
 * Here is a MultiplyConstant implementation in terms of raster.mapIfSet.
 */
case class MultiplyConstantMapIfSet(r:Op[Raster], c:Op[Int]) extends Op[Raster] {
  def _run(context:Context) = runAsync(r :: c :: Nil)

  final def _finish(raster:Raster, n:Int) = raster.mapIfSet(_ * n)

  val nextSteps:Steps = {
    case (raster:Raster) :: (n:Int) :: Nil => Result(_finish(raster, n))
  }
}

/**
 * Here is a MultiplyConstant implementation in terms of raster.mapIfSet and Op2.
 */
case class MultiplyConstantMapIfSetSugar(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.mapIfSet(_ * c))
})

/**
 * Here is a MultiplyConstant implementation in terms of raster.mapIfSet and Op2.
 */
case class MultiplyConstantMapSugar(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.map(z => if (z != NODATA) z * c else NODATA))
})

/**
 * Here is a MultiplyConstant implementation in terms of raster.mapIfSet and Op2.
 */
case class MultiplyConstantMapIfSetSugarWithLiteral(r:Op[Raster], c:Int) extends Op1(r)({
  r => Result(r.mapIfSet(_ * c))
})

/**
 * Here is a MultiplyConstant implementation in terms of a while-loop.
 */
case class MultiplyConstantWhileLoop(r:Op[Raster], c:Op[Int]) extends Op[Raster] {
  def _run(context:Context) = runAsync(r :: c :: Nil)

  val nextSteps:Steps = {
    case (raster:Raster) :: (n:Int) :: Nil => {
      val r2 = raster.copy
      val data = r2.data
      val len = r2.length
      var i = 0
      while (i < len) {
        val z = data(i)
        if (z != NODATA) data(i) = z * n
        i += 1
      }
      Result(r2)
    }
  }
}
