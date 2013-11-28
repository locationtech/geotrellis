package geotrellis.logic

import geotrellis._
import geotrellis.process._

/**
 * Conditionally executes one of two operations; if the Boolean Operation evaluates true, the first
 * Operation executes, otherwise the second Operation executes.
 */
case class If[A <: C,B <: C,C:Manifest](bOp: Op[Boolean], trueOp: Op[A], falseOp: Op[B])extends Op[C] {
  def _run() = runAsync('init :: bOp :: Nil)

  val nextSteps: Steps = {
    case 'init :: (b: Boolean) :: Nil => runAsync(
      List('result, 
           if (b) trueOp else falseOp))
    case 'result :: c :: Nil => Result(c.asInstanceOf[C])
  }
}
