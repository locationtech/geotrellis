package geotrellis.process.actors

import akka.actor._
import akka.routing._
import scala.concurrent.Await
import scala.concurrent.duration._

import geotrellis._
import geotrellis.process._

/**
 * Workers are responsible for evaluating an operation. However, if the
 * operation in question returns a step result that requires asynchronous callbacks, 
 * the work will be off-loaded to a StepAggregator.
 *
 * Thus, in practice workers only ever do work on simple operations.
 */
private[actors]
case class Worker(val server: Server) extends Actor {
  // Workers themselves don't have direct children. If the operation in
  // question has a step result requiring child operations be executed 
  // asynchronously, it will be processed by a StepAggregator
  // instead, who will be responsible for constructing the response (including
  // history).

  // Actor event loop
  def receive = {
    case RunOperation(incomingOp, pos, client, Some(ourDispatcher)) => {
      // If the the children of this operation should be run remotely,
      // replace our dispatcher with the remote dispatcher.
      val (op, dispatcher) = incomingOp match {
        case DispatchedOperation(runOp, newDispatcher) => (runOp, newDispatcher)
        case RemoteOperation(runOp, cluster) => throw new Exception("RemoteOperation should not be passed to worker")
        case _ => (incomingOp, ourDispatcher)
      }

      val handler = new ResultHandler(server,client,dispatcher,pos)
      val geotrellisContext = new Context(server)
      val history = History(op,server.externalId)


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
