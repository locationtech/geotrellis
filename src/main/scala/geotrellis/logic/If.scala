package geotrellis.logic

import geotrellis._
import geotrellis.process._

case class If[A <: C,B <: C,C:Manifest](bOp: Op[Boolean], trueOp: Op[A], falseOp: Op[B])extends Op[C] {
  def _run(context: Context) = runAsync('init :: bOp :: Nil)

  val nextSteps: Steps = {
    case 'init :: (b: Boolean) :: Nil => runAsync(
      List('result, 
           if (b) trueOp else falseOp))
    case 'result :: c :: Nil => Result(c.asInstanceOf[C])
  }
}
