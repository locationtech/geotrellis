package geotrellis.process.actors

import akka.actor._
import akka.routing._
import scala.concurrent.Await
import scala.concurrent.duration._

import geotrellis._
import geotrellis.process._

/**
 * This class contains functionality to handle results from operations,
 * evaluating StepOutput, constructing OperationResults and sending them 
 * back to the client.
 */
class ResultHandler(server:Server,
                    client:ActorRef,
                    dispatcher:ActorRef,
                    pos:Int) {
  // This method handles a given output. It will either return a result/error
  // to the client, or dispatch more asynchronous requests, as necessary.
  def handleResult[T](output:StepOutput[T],history:History) {
    output match {
      // ok, this operation completed and we have a value. so return it.
      case Result(value) => {
        val result = OperationResult(Complete(value, history.withResult(value)), pos)

        client ! result
      }

      // Execute the returned operation as the next step of this calculation.
      case AndThen(op) => {
         server.actor ! RunOperation(op, pos, client, Some(dispatcher))
      }

      // there was an error, so return that as well.
      case StepError(msg, trace) => {
        client ! OperationResult(Error(msg, history.withError(msg,trace)), pos)
      }

      // we need to do more work, so as the server to do it asynchronously.
      case StepRequiresAsync(args,cb) => {
        server.actor ! RunCallback(args, pos, cb, client, dispatcher,history)
      }
    }
  }
}

/**
 * Workers are responsible for evaluating an operation. However, if the
 * operation in question requires asynchronous callbacks, the work will be
 * off-loaded to a Calculation.
 *
 * Thus, in practice workers only ever do work on simple operations.
 */
case class Worker(val server: Server) extends Actor {
  // Workers themselves don't have direct children. If the operation in
  // question has child operations it will be processed by a Calculation
  // instead, who will be responsible for constructing the response (including
  // history).

  // Actor event loop
  def receive = {
    case RunOperation(incomingOp, pos, client, Some(ourDispatcher)) => {
      // If the the children of this operation should be run remotely,
      // replace our dispatcher with the remote dispatcher.
      val (op, dispatcher) = incomingOp match {
        case DispatchedOperation(runOp, newDispatcher) => (runOp, newDispatcher)
        case _ => (incomingOp, ourDispatcher)
      }

      val handler = new ResultHandler(server,client,dispatcher,pos)
      val geotrellisContext = new Context(server)

      val history = History(op)
      try {
        handler.handleResult(op.run(geotrellisContext),history)
      } catch {
        case e:Throwable => {
          val error = StepError.fromException(e)
          System.err.println("Operation failed, with exception: " + 
            s"${e}\n\nStack trace:\n${error.trace}\n", error.msg,error.trace)
          handler.handleResult(error,history)
        }
      }
    }
    case RunOperation(_,_,_,None) => sys.error("received msg without dispatcher")
    case x => sys.error(s"worker got unknown msg: $x")
  }
}

/** A calculation is a worker actor responsible for executing operations that are
  * dependent on the calculation of other operations. 
  */
case class Calculation[T](val server:Server, 
                          pos:Int, args:Args,
                          cb:Callback[T], 
                          client:ActorRef, 
                          dispatcher:ActorRef,
                          history:History)
extends Actor {

  val handler = new ResultHandler(server,client,dispatcher,pos)

  // These results won't (necessarily) share any type info with each other, so
  // we have to use Any as the least-upper type bound :(
  val results = Array.fill[Option[InternalCalculationResult[Any]]](args.length)(None)

  // Just after starting the actor, we need to dispatch out the child
  // operations to be run. If none of those existed, we should run the
  // callback and be done.
  override def preStart {
    for (i <- 0 until args.length) {
      args(i) match {
        case lit:Literal[_] =>
          val v = lit.value
          results(i) = Some(Complete(v,History.literal(v)))
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
    case OperationResult(childResult,  pos) => {
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

    case g => sys.error("calculation got unknown message: %s" format g)
  }
}
