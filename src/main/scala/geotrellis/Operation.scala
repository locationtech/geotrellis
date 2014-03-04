/*******************************************************************************
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package geotrellis

import geotrellis.process.LayerLoader

import scala.{PartialFunction => PF}

import akka.actor._

import scala.language.implicitConversions

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
   * Return operation identified.
   */
  private var _opId = getClass.getSimpleName
  def opId: String = _opId
  def withName(n:String):Operation[T] = { _opId += s" ($n)"; this }

  protected[geotrellis] def _run(): StepOutput[T]
  
  /**
   * Execute this operation and return the result.  
   */
  def run(): StepOutput[T] =
    _run()

  def runAsync(args:Args): StepOutput[T] = {
    StepRequiresAsync[T](args, { (nextArgs:Args) =>
      processNextSteps(nextArgs)
    })
  }

  def processNextSteps(args:Args):StepOutput[T] = nextSteps(args)

  /**
   * Create a new operation with a function that takes the result of this operation
   * and returns a new operation.
   */
  def flatMap[U](f:T=>Operation[U]):Operation[U] = new CompositeOperation(this,f)

  /**
   * Create a new operation that returns the result of the provided function that
   * takes this operation's result as its argument.
   */
  def map[U](f:(T)=>U):Operation[U] = logic.MapOp(this)(f)

  /**
   * Create an operation that applies the function f to the result of this operation,
   * but returns nothing.
   */
  def foreach[U](f:(T)=>U):Unit = map { t:T =>
    f(t)
    ()
  }

  def flatten[B](implicit f:T=>Op[B]) = flatMap(f(_))

  /**
   * Create a new operation with a function that takes the result of this operation
   * and returns a new operation.
   * 
   * Same as flatMap.
   */
  def withResult[U](f:T=>Operation[U]):Operation[U] = flatMap(f)


  def filter(f:(T) => Boolean) = flatMap { t =>
    if(f(t)) { this } else { FailOp[T]("The operation did not meet the required filter.") }
  }

  def withFilter(f:(T) => Boolean) = filter(f)

  def andThen[U](f:T => Op[U]) = flatMap(f)

  /** 
   * Call the given function with this operation as its argument.
   *
   * This is primarily useful for code readability.
   * @see http://debasishg.blogspot.com/2009/09/thrush-combinator-in-scala.html
   */
  def into[U] (f: (Operation[T]) => U):U = f(this)

  def prettyString:String = {
    val sb = new StringBuilder
    sb.append(s"$opId(")
    val arity = this.productArity
    for(i <- 0 until arity) {
      this.productElement(i) match {
        case lit:Literal[_] =>
          sb.append(s"LT{${lit.value}}")
        case op:Operation[_] =>
          sb.append(s"OP{${op.opId}}")
        case x => 
          sb.append(s"$x")
      }
      if(i < arity - 1) { sb.append(",") }
    }
    sb.append(")")
    sb.toString
  }
}


/**
 * Given an operation and a function that takes the result of that operation and returns
 * a new operation, return an operation of the return type of the function.
 * 
 * If the initial operation is g, you can think of this operation as f(g(x)) 
 */
case class CompositeOperation[+T,U](gOp:Op[U], f:(U) => Op[T]) extends Operation[T] {
  def _run() = runAsync('firstOp :: gOp :: Nil)

  val nextSteps:Steps = {
    case 'firstOp :: u :: Nil => runAsync('result :: f(u.asInstanceOf[U]) :: Nil) 
    case 'result :: t :: Nil => Result(t.asInstanceOf[T])
  } 
}

abstract class OperationWrapper[+T](op:Op[T]) extends Operation[T] {
  def _run() = op._run()
  val nextSteps:Steps = op.nextSteps
}

case class RemoteOperation[+T](val op:Op[T], cluster:Option[ActorRef])
extends OperationWrapper(op) {}

object Operation {
  implicit def implicitLiteralVal[A <: AnyVal](a:A)(implicit m:Manifest[A]):Operation[A] = Literal(a)
  implicit def implicitLiteralRef[A <: AnyRef](a:A):Operation[A] = Literal(a)
}

/** Operation that simply fails with the given message */
case class FailOp[T](msg:String) extends Operation[T] {
  def _run() = StepError(msg,"")
  val nextSteps:Steps = { case _ => StepError(msg,"") }
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

class Op0[T](f:()=>StepOutput[T]) extends Operation[T] {
  def productArity = 0
  def canEqual(other:Any) = other.isInstanceOf[Op0[_]]
  def productElement(n:Int) = throw new IndexOutOfBoundsException()

  def _run() = f()
  val nextSteps:Steps = {
    case _ => sys.error("should not be called")
  }
}

class Op1[A,T](a:Op[A])(f:(A)=>StepOutput[T]) extends Operation[T] {
  def _run() = runAsync(List(a))

  def productArity = 1
  def canEqual(other:Any) = other.isInstanceOf[Op1[_,_]]
  def productElement(n:Int) = if (n == 0) a else throw new IndexOutOfBoundsException()
  val myNextSteps:PartialFunction[Any,StepOutput[T]] = {
    case a :: Nil => f(a.asInstanceOf[A])
  }
  val nextSteps = myNextSteps
}

class Op2[A,B,T](a:Op[A], b:Op[B]) (f:(A,B)=>StepOutput[T]) extends Operation[T] {
  def productArity = 2
  def canEqual(other:Any) = other.isInstanceOf[Op2[_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => a
    case 1 => b
    case _ => throw new IndexOutOfBoundsException()
  }
  def _run() = runAsync(List(a,b))
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
  def _run() = runAsync(List(a,b,c))
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
  def _run() = runAsync(List(a,b,c,d))
  val nextSteps:Steps = { 
    case a :: b :: c :: d :: Nil => {
      f(a.asInstanceOf[A], b.asInstanceOf[B], c.asInstanceOf[C], d.asInstanceOf[D])
    }
  }

}

abstract class Op5[A,B,C,D,E,T](a:Op[A],b:Op[B],c:Op[C],d:Op[D],e:Op[E])
(f:(A,B,C,D,E)=>StepOutput[T]) extends Operation[T] {
  def _run() = runAsync(List(a,b,c,d,e))
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
  def _run() = runAsync(List(a,b,c,d,e,f))
  val nextSteps:Steps = {
    case a :: b :: c :: d :: e :: f :: Nil => {
      ff(a.asInstanceOf[A], b.asInstanceOf[B], c.asInstanceOf[C],
         d.asInstanceOf[D], e.asInstanceOf[E], f.asInstanceOf[F])
    }
  }
}
