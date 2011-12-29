package trellis.operation

import trellis.process.{Server,CreateTaskGroup,Results}
import PackageDummy._
import trellis.process.RunAsync
import trellis.process.InProgress
import trellis.process.CalculationResult
import trellis.process.Complete

object PackageDummy {
}



/**
 * Base Operation for all Trellis functionality. All other operations must
 * extend this trait.
 */
trait Operation[T] {
  val nextSteps:PartialFunction[Any,Option[T]]
  type Steps = PartialFunction[Any,Option[T]]
  type Callback = (Option[T]) => Unit
  var startTime:Long = 0
  var endTime:Long = 0

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

  protected def _run(server:Server, callback:Callback):Unit 
 
  /**
   * Execute this operation and return the result.  
   */
  def run(server:Server, callback:Callback): Unit = {
    startTime = System.currentTimeMillis()
    this._run(server, callback)
  }

  def runAsync( args:List[Any], server:Server, callback:(Option[T]) => Any) = {
    val f = (result:CalculationResult[Any]) => { 
      val oldResult:Option[T] = result match {
        case Complete(v) => Some(v.asInstanceOf[T])
        case _ => None
      }
      callback(oldResult)
    }
    server.actor ! RunAsync(args, nextStep(f, _:Any)) 
    None
  }

  // TODO: we probably shouldn't use response:Any
  def nextStep (callback:(CalculationResult[_])=>Any, response:Any) {
    response match {
      // If we've received Some() response, invoke the callback.
      // Otherwise we have just completed a single async step in the process,
      // and suspend execution here.
      case Complete(value) => nextSteps(Results(value.asInstanceOf[List[Any]])) match {
        case Some(v) => callback(Complete(v))
        case None => { println("DEBUG: received None from op step in: " + this )}
      }
      case InProgress => println("debug: in progress in next step")
      case u => throw new Exception("got something unexpected: %s".format(u))
    }
  }

  //def call[U:Manifest] (f:T => U) = Call(this, f)
}

object Operation {
  implicit def implicitLiteral[A](a:A):Operation[A] = Literal(a)
}


trait SimpleOperation[T] extends Operation[T] {
  def _value(server:Server):T // define simple behavior here

  def _run(server:Server, callback:(Option[T]) => Unit) = {
    startTime = System.currentTimeMillis()
    val value = this._value(server)
    endTime = System.currentTimeMillis()
    callback(Some(value))
  }
  val nextSteps:Steps = {case _ => throw new Exception("simple operation has no steps") }
}
