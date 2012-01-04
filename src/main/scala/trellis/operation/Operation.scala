package trellis.operation

import trellis.process._

import scala.{PartialFunction => PF}

/**
 * Base Operation for all Trellis functionality. All other operations must
 * extend this trait.
 */
trait Operation[T] {
  type Steps = PF[Any, StepOutput[T]]

  val nextSteps:PF[Any, StepOutput[T]]

  val debug = false
  private def log(msg:String) = if(debug) println(msg)

  /**
    * Return operation identified (class simple name).
    */
  def name: String = getClass.getSimpleName

  protected def _run(context:Context): StepOutput[T]
 
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

  //def call[U:Manifest] (f:T => U) = Call(this, f)
}

object Operation {
  implicit def implicitLiteral[A](a:A):Operation[A] = Literal(a)
}


trait SimpleOperation[T] extends Operation[T] {
  // define simple behavior here
  def _value(context:Context):T 

  def _run(context:Context) = Result(this._value(context))

  val nextSteps:PF[Any, Result[T]] = {
    case _ => throw new Exception("simple operation has no steps")
  }
}
