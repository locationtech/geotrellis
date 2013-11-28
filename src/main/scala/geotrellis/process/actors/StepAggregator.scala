package geotrellis.process.actors

import akka.actor._
import akka.routing._
import scala.concurrent.Await
import scala.concurrent.duration._

import geotrellis._
import geotrellis.process._


/** 
 * A StepAggregator is responsible for executing 
 * operations in a list of arguments that are returned
 * from a step in an Operation's execution. Each operation
 * is sent to the server for asynchronous execution.
 * Once all the operations are completed and the results
 * aggregated, a callback (the next step of the operation)
 * is executed and the results are handled by a ResultHandler.
 */
private[actors]
case class StepAggregator[T](val server:Server, 
                             pos:Int, 
                             args:Args,
                             cb:Callback[T], 
                             client:ActorRef, 
                             dispatcher:ActorRef,
                             history:History)
    extends Actor {

  val handler = new ResultHandler(server,client,dispatcher,pos)

  // These results won't (necessarily) share any type info with each other, so
  // we have to use Any as the least-upper type bound :(
  val results = Array.fill[Option[InternalOperationResult[Any]]](args.length)(None)

  // Just after starting the actor, we need to dispatch out the child
  // operations to be run. If none of those existed, we should run the
  // callback and be done.
  override def preStart {
    for (i <- 0 until args.length) {
      args(i) match {
        case lit:Literal[_] =>
          val v = lit.value
          results(i) = Some(Complete(v,History.literal(v,server.externalId)))
        case op:Operation[_] =>
          dispatcher ! RunOperation(op, i, self, None)
        case value =>
          results(i) = Some(Inlined(value))
      }
    }

    if (isDone) { 
      finishCallback()
      context.stop(self)
    }
  }

  // This should create a list of all the (non-trivial) child histories we
  // have. This leaves out inlined arguments, who don't have history in any
  // real sense (e.g. they were complete when we received them).
  def childHistories = 
    results.toList.flatMap {
      case Some(Complete(_, t)) => Some(t)
      case Some(Error(_, t)) => Some(t)
      case Some(Inlined(_)) => None
      case None => None
    }

  // If any entry in the results array is null, we're not done.
  def isDone = results.find(_ == None).isEmpty

  def hasError = results.find { case Some(Error(_,_)) => true; case a => false }.isDefined

  // Create a list of the actual values of our children.
  def getValues = results.toList.map {
    case Some(Complete(value, _)) => value
    case Some(Inlined(value)) => value
    case r => sys.error("found unexpected result (some(error)) ") 
  }

  // This is called when we have heard back from all our sub-operations and
  // are ready to begin evaluation. After this point we will terminate and not
  // receive any more messages.
  def finishCallback() {
    try {
      handler.handleResult(cb(getValues),history.withStep(childHistories))
    } catch {
      case e:Throwable => {
        val error = StepError.fromException(e)
        System.err.println(s"Operation failed, with exception: $e\n\nStack trace:${error.trace}\n\n")
        handler.handleResult(error,history.withStep(childHistories))
      }
    }
    context.stop(self)
  }

  // Actor event loop
  def receive = {
    case PositionedResult(childResult,  pos) => {
      results(pos) = Some(childResult)
      if (!isDone) {
      } else if (hasError) {
        val se = StepError("error", "error")
        handler.handleResult(se,history.withStep(childHistories))
        context.stop(self)
      } else {
        finishCallback()
        context.stop(self)
      }
    }

    case g => sys.error(s"${this.getClass.getSimpleName} got unknown message: %s" format g)
  }
}
