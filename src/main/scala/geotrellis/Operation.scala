package geotrellis

import geotrellis.process._
import scala.{PartialFunction => PF}

import akka.actor._

/**
 * Base Operation for all GeoTrellis functionality. All other operations must
 * extend this trait.
 */
abstract class Operation[+T] extends Product with Serializable {
  type Steps = PartialFunction[Any, StepOutput[T]]
  type Args = List[Any]
  val nextSteps:PartialFunction[Any,StepOutput[T]]

  val debug = false
  private def log(msg:String) = if(debug) println(msg)

  /**
    * Return operation identified (class simple name).
    */
  def name: String = getClass.getSimpleName

  protected[geotrellis] def _run(context:Context): StepOutput[T]
 
  /**
   * Execute this operation and return the result.  
   */
  def run(context:Context): StepOutput[T] = {
    val o = _run(context)
    o
  }

  def runAsync(args:Args): StepOutput[T] = {
    val f = (args2:Args) => {
      val stepOutput:StepOutput[T] = processNextSteps(args2)
      stepOutput
    }
    val o = StepRequiresAsync[T](args, f)
    o
  }

  def processNextSteps(args:Args):StepOutput[T] = nextSteps(args)

  def dispatch(dispatcher:ActorRef) = {
    DispatchedOperation(this, dispatcher)    
  }

 /**
  * Create a new operation with a function that takes the result of this operation
  * and returns a new operation.
  */
 def flatMap[U](f:T=>Operation[U]):Operation[U] = new CompositeOperation(this,f) 

 /**
  * Create a new operation that returns the result of the provided function that
  * takes this operation's result as its argument.
  */
 def map[U](f:(T)=>U):Operation[U] = logic.Do1(this)(f) 

 /**
  * Create an operation that applies the function f to the result of this operation,
  * but returns nothing.
  */
 def foreach[U](f:(T)=>U):Unit = logic.Do1(this) {
    (t:T) => {
      f(t)
      ()
    }
 }

 /**
  * Create a new operation with a function that takes the result of this operation
  * and returns a new operation.
  * 
  * Same as flatMap.
  */
 def withResult[U](f:T=>Operation[U]):Operation[U] = flatMap(f)


 //TODO: how should filter be implemented for list comprehensions?
 def filter(f:(T) => Boolean) = this
}


/**
 * Given an operation and a function that takes the result of that operation and returns
 * a new operation, return an operation of the return type of the function.
 * 
 * If the initial operation is g, you can think of this operation as f(g(x)) 
 */
case class CompositeOperation[+T,U](gOp:Op[U], f:(U) => Op[T]) extends Operation[T] {
  def _run(context:Context) = runAsync('firstOp :: gOp :: Nil)

  val nextSteps:Steps = {
    case 'firstOp :: u :: Nil => runAsync('result :: f(u.asInstanceOf[U]) :: Nil) 
    case 'result :: t :: Nil => Result(t.asInstanceOf[T])
  } 
}

abstract class OperationWrapper[+T](op:Op[T]) extends Operation[T] {
  def _run(context:Context) = op._run(context)
  val nextSteps:Steps = op.nextSteps
}

case class DispatchedOperation[+T](val op:Op[T], val dispatcher:ActorRef)
extends OperationWrapper(op) {}

object Operation {
  implicit def implicitLiteral[A:Manifest](a:A):Operation[A] = Literal(a)
}

/**
 * When run, Operations will return a StepOutput. This will either indicate a
 * complete result (Result), an error (StepError), or indicate that it
 * needs other work performed asynchronously before it can continue.
 */
sealed trait StepOutput[+T]

case class Result[T](value:T) extends StepOutput[T]
case class AndThen[T](op:Operation[T]) extends StepOutput[T]
case class StepError(msg:String, trace:String) extends StepOutput[Nothing]
case class StepRequiresAsync[T](args:Args, cb:Callback[T]) extends StepOutput[T]

object StepError { 
  def fromException(e:Throwable) = { 
    val msg = e.getMessage
    val trace = e.getStackTrace.map(_.toString).mkString("\n") 
    StepError(msg, trace) 
  } 
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

class Op1[A,T](a:Op[A])(f:(A)=>StepOutput[T]) extends Operation[T] {
  def _run(context:Context) = runAsync(List(a))

  def productArity = 1
  def canEqual(other:Any) = other.isInstanceOf[Op1[_,_]]
  def productElement(n:Int) = if (n == 0) a else throw new IndexOutOfBoundsException()
  val myNextSteps:PartialFunction[Any,StepOutput[T]] = {
    case a :: Nil => f(a.asInstanceOf[A])
  }
  val nextSteps = myNextSteps
}

object OpX {
  /**
   * Add simple syntax for creating an operation.
   *
   * Define a function after op function that returns:
   * 
   * 1) A literal value, e.g. 
   *   val PlusOne = op { (i:Int) => i + 1 }
   *
   * 2) An operation to be executed:
   *   val LocalPlusOne ( (r:Raster, i:Int) => local.Add(r,i + 1) )
   *
   * 3) Or a StepResult (which indicates success or failure)
   *   val PlusOne = op { (i:Int) => Result(i + 1) }
   *
   */
  type DI = DummyImplicit

  
  // Op1 methods for op //

  /**
   * Create an operation from a 1-arg function that returns StepOutput. 
   * 
   * For example:
   *
   * val PlusOne = op { (i:Int) => Result(i + 1) }
   */
  def op[A,T](f:(A)=>StepOutput[T])(implicit m:Manifest[T]):(Op[A]) => Op1[A,T] = 
    (a:Op[A]) => new Op1(a)((a) => f(a))
 
  /**
   * Create an operation from a 1-arg function that returns an operation to be executed.
   *
   * For example:
   *
   * val LocalPlusOne ( (r:Raster, i:Int) => local.Add(r,i + 1) )
   *
   */
  def op[A,T](f:(A)=>Op[T])(implicit m:Manifest[T], n:DI):(Op[A]) => Op1[A,T] = 
    (a:Op[A]) => new Op1(a)((a) => StepRequiresAsync(List(f(a)), (l) => Result(l.head.asInstanceOf[T])))
 
  /**
   * Create an operation from a 1-arg function that returns a literal value.
   *
   * For example:
   * 
   * val PlusOne = op { (i:Int) => i + 1 }
   */ 
  def op[A,T](f:(A)=>T)(implicit m:Manifest[T], n:DI, o:DI):(Op[A]) => Op1[A,T] = 
    (a:Op[A]) => new Op1(a)((a) => Result(f(a)))


  // Op2 methods for op() //

  /**
   * Create an operation from a 2-arg function that returns StepOutput. 
   */ 
  def op[A,B,T](f:(A,B)=>StepOutput[T])(implicit m:Manifest[T]):(Op[A],Op[B]) => Op2[A,B,T] = 
    (a:Op[A],b:Op[B]) => new Op2(a,b)((a,b) => f(a,b))

  /**
   * Create an operation from a 2-arg function that returns an operation.
   */ 
  def op[A,B,T](f:(A,B)=>Op[T])(implicit m:Manifest[T], n:DI):(Op[A],Op[B]) => Op2[A,B,T] = 
    (a:Op[A],b:Op[B]) => new Op2(a,b)((a,b) => StepRequiresAsync(List(f(a,b)), (l) => Result(l.head.asInstanceOf[T])))
 
  /**
   * Create an operation from a 2-arg function that returns a literal value.
   */ 
  def op[A,B,T](f:(A,B)=>T)(implicit m:Manifest[T], n:DI, o:DI):(Op[A],Op[B]) => Op2[A,B,T] = 
    (a:Op[A],b:Op[B]) => new Op2(a,b)((a,b) => Result(f(a,b)))
 

  // Op3 methods for op() //

  /**
   * Create an operation from a 3-arg function that returns StepOutput. 
   */
  def op[A,B,C,T](f:(A,B,C)=>StepOutput[T])(implicit m:Manifest[T]):(Op[A],Op[B],Op[C]) => Op3[A,B,C,T] =
    (a:Op[A],b:Op[B],c:Op[C]) => new Op3(a,b,c)((a,b,c) => f(a,b,c))

  /**
   * Create an operation from a 3-arg function that returns an operation.
   */
  def op[A,B,C,T](f:(A,B,C)=>Op[T])(implicit m:Manifest[T], n:DI):(Op[A],Op[B],Op[C]) => Op3[A,B,C,T] =
    (a:Op[A],b:Op[B],c:Op[C]) => new Op3(a,b,c)((a,b,c) => StepRequiresAsync(List(f(a,b,c)), (l) => Result(l.head.asInstanceOf[T])))

  /**
   * Create an operation from a 3-arg function that returns a literal value.
   */
  def op[A,B,C,T](f:(A,B,C)=>T)(implicit m:Manifest[T], n:DI, o:DI):(Op[A],Op[B],Op[C]) => Op3[A,B,C,T] =
    (a:Op[A],b:Op[B],c:Op[C]) => new Op3(a,b,c)((a,b,c) => Result(f(a,b,c)))
 
  // Op4 methods for op() //

  /**
   * Create an operation from a 4-arg function that returns StepOutput. 
   */
  def op[A,B,C,D,T](f:(A,B,C,D)=>StepOutput[T])(implicit m:Manifest[T]):(Op[A],Op[B],Op[C],Op[D]) => Op4[A,B,C,D,T] =
    (a:Op[A],b:Op[B],c:Op[C],d:Op[D]) => new Op4(a,b,c,d)((a,b,c,d) => f(a,b,c,d))

  /**
   * Create an operation from a 4-arg function that returns an operation.
   */
  def op[A,B,C,D,T](f:(A,B,C,D)=>Op[T])(implicit m:Manifest[T], n:DI):(Op[A],Op[B],Op[C],Op[D]) => Op4[A,B,C,D,T] =
    (a:Op[A],b:Op[B],c:Op[C],d:Op[D]) => new Op4(a,b,c,d)((a,b,c,d) => StepRequiresAsync(List(f(a,b,c,d)), (l) => Result(l.head.asInstanceOf[T])))

  /**
   * Create an operation from a 4-arg function that returns a literal value.
   */
  def op[A,B,C,D,T](f:(A,B,C,D)=>T)(implicit m:Manifest[T], n:DI, o:DI):(Op[A],Op[B],Op[C],Op[D]) => Op4[A,B,C,D,T] =
    (a:Op[A],b:Op[B],c:Op[C],d:Op[D]) => new Op4(a,b,c,d)((a,b,c,d) => Result(f(a,b,c,d)))
 
}

class Op2[A,B,T](val a:Op[A], val b:Op[B]) (f:(A,B)=>StepOutput[T]) extends Operation[T] {
  def productArity = 2
  def canEqual(other:Any) = other.isInstanceOf[Op2[_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => a
    case 1 => b
    case _ => throw new IndexOutOfBoundsException()
  }
  def _run(context:Context) = runAsync(List(a,b))
  val nextSteps:Steps = { 
    case a :: b :: Nil => f(a.asInstanceOf[A], b.asInstanceOf[B])
  }

}


class Op3[A,B,C,T](a:Op[A],b:Op[B],c:Op[C])
(f:(A,B,C)=>StepOutput[T]) extends Operation[T] {
  def productArity = 3
  def canEqual(other:Any) = other.isInstanceOf[Op3[_,_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => a
    case 1 => b
    case 2 => c
    case _ => throw new IndexOutOfBoundsException()
  }
  def _run(context:Context) = runAsync(List(a,b,c))
  val nextSteps:Steps = { 
    case a :: b :: c :: Nil => {
      f(a.asInstanceOf[A], b.asInstanceOf[B], c.asInstanceOf[C])
    }
  }
}

class Op4[A,B,C,D,T](a:Op[A],b:Op[B],c:Op[C],d:Op[D])
(f:(A,B,C,D)=>StepOutput[T]) extends Operation[T] {
  def productArity = 4
  def canEqual(other:Any) = other.isInstanceOf[Op4[_,_,_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => a
    case 1 => b
    case 2 => c
    case 3 => d
    case _ => throw new IndexOutOfBoundsException()
  }
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
