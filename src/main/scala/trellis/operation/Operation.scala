package trellis.operation

import trellis.process._
import trellis._
import scala.{PartialFunction => PF}

import akka.actor._

/**
 * Base Operation for all Trellis functionality. All other operations must
 * extend this trait.
 */
abstract class Operation[T] extends Product {
  type Steps = PF[Any, StepOutput[T]]

  val nextSteps:PF[Any, StepOutput[T]]

  val debug = false
  private def log(msg:String) = if(debug) println(msg)

  /**
    * Return operation identified (class simple name).
    */
  def name: String = getClass.getSimpleName

  protected[operation] def _run(context:Context): StepOutput[T]
 
  /**
   * Execute this operation and return the result.  
   */
  def run(context:Context): StepOutput[T] = {
    log("Operation.run called")
    val o = _run(context)
    log("Operation run returning %s" format o)
    o
  }

  def runAsync(args:Args): StepOutput[T] = {
    log("Operation.runAsync called with %s" format args)
    val f = (args2:Args) => {
      log("*** runAsync-generated callback called with %s" format args2)
      val stepOutput:StepOutput[T] = nextSteps(args2)
      log("*** step output from nextSteps was %s" format stepOutput)
      stepOutput
    }
    val o = StepRequiresAsync[T](args, f)
    log("Operation.runAsync returns %s" format o)
    o
  }

  def dispatch(dispatcher:ActorRef) = {
    DispatchedOperation(this, dispatcher)    
  }
}

abstract class OperationWrapper[T](op:Op[T]) extends Operation[T] {
  def _run(context:Context) = op._run(context)
  val nextSteps:Steps = op.nextSteps
}

case class DispatchedOperation[T](val op:Op[T], val dispatcher:ActorRef)
extends OperationWrapper(op) {}

object Operation {
  implicit def implicitLiteral[A:Manifest](a:A):Operation[A] = Literal(a)
}



/**
 * Below are the Op0 - Op6 abstract classes.
 *
 * These are useful for easily defining operations which just want to evaluate
 * their child operations and then run a single function afterwards.
 *
 * For example:
 *
 * case class Add2(x:Op[Int], y:Op[Int]) extends Op2(x, y)(_ + _)
 */

abstract class Op0[T](f:()=>StepOutput[T]) extends Operation[T] {
  def _run(context:Context) = f()
  val nextSteps:Steps = {
    case _ => sys.error("should not be called")
  }
}

abstract class Op1[A,T](a:Op[A])(f:(A)=>StepOutput[T]) extends Operation[T] {
  def _run(context:Context) = runAsync(List(a))
  val nextSteps:Steps = {
    case a :: Nil => f(a.asInstanceOf[A])
  }
}

abstract class Op2[A,B,T](a:Op[A],b:Op[B])
(f:(A,B)=>StepOutput[T]) extends Operation[T] {
  def _run(context:Context) = runAsync(List(a,b))
  val nextSteps:Steps = { 
    case a :: b :: Nil => f(a.asInstanceOf[A], b.asInstanceOf[B])
  }
}


abstract class Op3[A,B,C,T](a:Op[A],b:Op[B],c:Op[C])
(f:(A,B,C)=>StepOutput[T]) extends Operation[T] {
  def _run(context:Context) = runAsync(List(a,b,c))
  val nextSteps:Steps = { 
    case a :: b :: c :: Nil => {
      f(a.asInstanceOf[A], b.asInstanceOf[B], c.asInstanceOf[C])
    }
  }
}

abstract class Op4[A,B,C,D,T](a:Op[A],b:Op[B],c:Op[C],d:Op[D])
(f:(A,B,C,D)=>StepOutput[T]) extends Operation[T] {
  def _run(context:Context) = runAsync(List(a,b,c,d))
  val nextSteps:Steps = { 
    case a :: b :: c :: d :: Nil => {
      f(a.asInstanceOf[A], b.asInstanceOf[B], c.asInstanceOf[C], d.asInstanceOf[D])
    }
  }
}

abstract class Op5[A,B,C,D,E,T](a:Op[A],b:Op[B],c:Op[C],d:Op[D],e:Op[E])
(f:(A,B,C,D,E)=>StepOutput[T]) extends Operation[T] {
  def _run(context:Context) = runAsync(List(a,b,c,d,e))
  val nextSteps:Steps = {
    case a :: b :: c :: d :: e :: Nil => {
      f(a.asInstanceOf[A], b.asInstanceOf[B], c.asInstanceOf[C],
        d.asInstanceOf[D], e.asInstanceOf[E])
    }
  }
}

abstract class Op6[A,B,C,D,E,F,T]
(a:Op[A],b:Op[B],c:Op[C],d:Op[D],e:Op[E],f:Op[F])
(ff:(A,B,C,D,E,F)=>StepOutput[T]) extends Operation[T] {
  def _run(context:Context) = runAsync(List(a,b,c,d,e,f))
  val nextSteps:Steps = {
    case a :: b :: c :: d :: e :: f :: Nil => {
      ff(a.asInstanceOf[A], b.asInstanceOf[B], c.asInstanceOf[C],
         d.asInstanceOf[D], e.asInstanceOf[E], f.asInstanceOf[F])
    }
  }
}
