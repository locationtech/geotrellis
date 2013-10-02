package geotrellis

import geotrellis.process._

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
