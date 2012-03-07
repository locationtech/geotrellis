package geotrellis.operation

import geotrellis._

import geotrellis.process._

import scala.math.{max, min, pow}

/**
 * Useful wrapper for Function1[Int,Int] that helps preserve type information
 * between actors.
 */
case class UnaryF(f:(Int) => Int) extends Function1[Int, Int] {
  def apply(z:Int) = f(z)
}

object UnaryF {
  def id = UnaryF(identity _)
}

object UnaryLocal {
  type Steps = PartialFunction[Any, StepOutput[UnaryF]]
}

/**
 * Abstract class for all operations that are unary (operate on a single raster) and
 * are local (operate on each cell without knowledge of other cells).
 */
trait UnaryLocal extends Op[IntRaster] with LocalOp {
  def r:Op[IntRaster]

  def getUnderlyingOp:Op[IntRaster] = r match {
    case (child:UnaryLocal) => child.getUnderlyingOp
    case child => child
  }

  def _run(context:Context) = runAsync(GetUnaryFunction(this) :: getUnderlyingOp :: Nil)

  val nextSteps:Steps = {
    case UnaryF(f) :: (raster:IntRaster) :: Nil => Result(raster.mapIfSet(f))
  }
  
  def functionOps:List[Any]
  val functionNextSteps:UnaryLocal.Steps
}

case class GetUnaryFunction (op:UnaryLocal) extends Op[UnaryF] {
  def _run(context:Context) = op.r match {
    case u:UnaryLocal => runAsync(GetUnaryFunction(u) :: op.functionOps)
    case _ => runAsync(UnaryF.id :: op.functionOps)
  }
  val nextSteps = op.functionNextSteps
}

trait SimpleUnaryLocal extends UnaryLocal {
  def handleCell(z:Int):Int
  def functionOps:List[Any] = Nil
  val functionNextSteps:UnaryLocal.Steps = {
    case _ => Result(UnaryF(handleCell _))
  }
}
