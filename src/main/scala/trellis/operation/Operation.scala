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
  var startTime:Long = 0
  var endTime:Long = 0

  val debug = false
  private def log(msg:String) = if(debug) println(msg)

  /**
    * Return total execution time of operation.
    */
  def totalTime = endTime - startTime

  /**
    * Return exclusive execution time of operation.
    */
  def exclusiveTime = {
    val deps = childOperations.filter(_.startTime >= this.startTime)
    totalTime - deps.map(_.totalTime).foldLeft(0.toLong)((a, b) => a + b)
  }

  /**
    * Return child operations (operations invoked by this operation). 
    */
  def childOperations: Seq[Operation[_]]

  /**
    * Return operation identified (class simple name).
    */
  def opId: String = getClass.getSimpleName

  /**
    * Print timing tree for diagnostics.
    */
  def logTimingTree { Console.printf(this.genTimingTree) }

  /**
    * Return timing tree as a string, for diagnostics.
    */
  def genTimingTree = _genTimingTree(0)

  protected def _genTimingTree(lvl:Int):String = {
    var s = "%-30s  %5d ms   %5d ms\n".format((" " * lvl) + opId, totalTime, exclusiveTime)
    childOperations.foreach {
      c => s = s + c._genTimingTree(lvl + 1)
    }
    s
  }

  protected def _run(server:Server)(implicit timer:Timer): StepOutput[T]
 
  /**
   * Execute this operation and return the result.  
   */
  def run(server:Server)(implicit timer:Timer): StepOutput[T] = {
    log("Operation.run called")
    startTime = System.currentTimeMillis()
    val o = this._run(server)(timer)
    log("Operation run returning %s" format o)
    o
  }

  def runAsync(args:Args, server:Server): StepOutput[T] = {
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
  def _value(server:Server)(implicit timer:Timer):T 

  def _run(server:Server)(implicit timer:Timer) = {
    startTime = System.currentTimeMillis()
    val value = this._value(server)
    endTime = System.currentTimeMillis()
    StepResult(value)
  }

  val nextSteps:PF[Any, StepResult[T]] = {
    case _ => throw new Exception("simple operation has no steps")
  }
}
