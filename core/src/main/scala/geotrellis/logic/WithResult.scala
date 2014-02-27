package geotrellis.logic

import geotrellis._
import geotrellis.process._

/**
 * Run a function on the result of an operation
 *
 * Functionally speaking, this represents the bind operation
 * on the Operation monad
 */
case class WithResult[A, Z](a:Op[A])(call:A => Op[Z]) extends Op[Z] {
  def _run() = runAsync(a.flatMap(call) :: Nil)
  val nextSteps:Steps = {
    case a :: Nil => Result(a.asInstanceOf[Z])
  }
}
