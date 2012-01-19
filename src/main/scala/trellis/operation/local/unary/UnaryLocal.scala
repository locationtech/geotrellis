package trellis.operation

import trellis._

import trellis.process._

import scala.math.{max, min, pow}

object UnaryLocal {
  type Steps = PartialFunction[Any, StepOutput[(Int) => Int]]
}

/**
 * Abstract class for all operations that are unary (operate on a single raster) and
 * are local (operate on each cell without knowledge of other cells).
 */
trait UnaryLocal extends Op[IntRaster] with LocalOp {
  val r:Op[IntRaster]

  def getUnderlyingOp:Op[IntRaster] = r match {
    case (child:UnaryLocal) => child.getUnderlyingOp
    case child => child
  }

  def _run(context:Context) = runAsync(GetUnaryFunction(this) :: getUnderlyingOp :: Nil)

  // cast a Function1[_,_] into a Function1[Int,Int]
  def asF1(f:Function1[_,_]) = f.asInstanceOf[Function1[Int, Int]]

  val nextSteps:Steps = {
    case (f:Function1[_, _]) :: (raster:IntRaster) :: Nil => Result(raster.mapIfSet(asF1(f)))
  }
  
  def functionOps:List[Any]
  val functionNextSteps:UnaryLocal.Steps
}

trait SimpleUnaryLocal extends UnaryLocal {
  def getCallback:(Int) => Int
  def functionOps:List[Any] = Nil
  val functionNextSteps:UnaryLocal.Steps = {
    case _ => Result(getCallback)
  }
}
case class GetUnaryFunction (op:UnaryLocal) extends Op[(Int) => Int] {
  def _run(context:Context) = op.r match {
    case u:UnaryLocal => runAsync(GetUnaryFunction(u) :: op.functionOps)
    case _ => runAsync(((z:Int) => z) :: op.functionOps)
  }
  val nextSteps = op.functionNextSteps
}
