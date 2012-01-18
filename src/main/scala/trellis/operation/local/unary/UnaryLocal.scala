package trellis.operation

import trellis._

import trellis.process._

import scala.math.{max, min, pow}

/**
 * Abstract class for all operations that are unary (operate on a single raster) and
 * are local (operate on each cell without knowledge of other cells).
 */
trait UnaryLocal extends Op[IntRaster] with LocalOp {
  val r:Op[IntRaster]

  //def getCallback(context:Context): (Int) => Int

  def getUnderlyingOperation:Op[IntRaster] = {
    r match {
      case (child:UnaryLocal) => child.getUnderlyingOperation
      case child => child
    }
  }

  def _run(context:Context) = runAsync(GetUnaryFunction(this) :: context :: Nil)
  
  val nextSteps:Steps = {
    case (f:Function1[_, _]) :: (context:Context) :: Nil => {
      r match {
        case (child:UnaryLocal) => { 
          runAsync( 'merge :: GetUnaryFunction(child) :: f :: Literal( child.getUnderlyingOperation ) :: Nil) 
        }
        case child => runAsync('map :: child :: f :: Nil) //.mapIfSet(f)
      }
    }
    case 'merge :: (childF:Function1[_,_]) :: (f:Function1[_,_]) :: (op:Operation[_]) :: Nil => {
      val doCell = DoCell(op.asInstanceOf[Op[IntRaster]], (z:Int) => 
        f.asInstanceOf[Function1[Int,Int]](childF.asInstanceOf[Function1[Int,Int]](z)) )
      runAsync( 'done :: doCell :: Nil )
    }
    case 'done :: (raster:IntRaster) :: Nil => Result(raster)
    case 'map :: (raster:IntRaster) :: (f:Function1[_,_]) :: Nil => Result(raster.mapIfSet(f.asInstanceOf[Function[Int,Int]]))
  }
  
  def functionOps:List[Op[Any]]
  val functionNextSteps:PartialFunction[Any,StepOutput[Function1[Int,Int]]]
}

trait SimpleUnaryLocal extends UnaryLocal {
  def getCallback: (Int) => Int
  def functionOps:List[Op[Any]] = Nil
  val functionNextSteps:PartialFunction[Any,StepOutput[Function1[Int,Int]]] = {
    case _ => Result(getCallback)
  }
}
case class GetUnaryFunction (op:UnaryLocal) extends Op[(Int) => Int] {
  def _run(context:Context) = {
    val ops:List[Op[Any]] = op.functionOps
    runAsync(op.functionOps)
  }
  val nextSteps = op.functionNextSteps
}

