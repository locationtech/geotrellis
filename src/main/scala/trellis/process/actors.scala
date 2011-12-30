package trellis.process

// akka imports
import akka.actor._
import akka.routing._
import akka.dispatch.Await
import akka.util.duration._

import trellis.operation.Operation

/**
 * History stores information about the execution of an operation.
 */
sealed trait History {
  def id:String
  def info:Map[String, Any] = Map.empty[String, Any]

  def startTime:Long
  def stopTime:Long
  def children:List[History]

  def isOriginalFailure = originalFailures == List(this)

  def originalFailures:List[Failure] = this match {
    case t:Success => Nil
    case t:Failure => children.flatMap(_.originalFailures) match {
      case Nil => List(t)
      case failures => failures
    }
  }
}

/**
 * History of a successful operation.
 */
case class Success(id:String, startTime:Long, stopTime:Long,
                   children:List[History]) extends History

/**
 * History of a failed operation.
 */
case class Failure(id:String, startTime:Long, stopTime:Long,
                   children:List[History], message:String,
                   trace:String) extends History {}


/**
 * CalculationResult contains an operation's results.
 */
sealed trait CalculationResult[+T] {
  def history:History
}

case class Inlined[T](value:T) extends CalculationResult[T] {
  def history = sys.error("inlined results have no history")
}

/**
 * CalculationResult for a successful operation.
 */
case class Complete[T](value:T, history:Success) extends CalculationResult[T]

/**
 * CalculationResult for a failed operation.
 */
case class Error(message:String, history:Failure) extends CalculationResult[Nothing]


/**
 * External messages sent from Trellis land (non-actors)
 */

/**
 * External message to compute the given operation and return result to sender.
 */
case class Run(op:Operation[_])

/**
 * Internal message to run the provided op and send the result to the client.
 */
case class RunOperation[T](op: Operation[T], pos: Int, client: ActorRef)

/**
 * Internal message to compute the provided args (if necessary), invoke the
 * provided callback with the computed args, and send the result to the client.
 */
case class RunCallback[T](args:Args, pos:Int, cb:Callback[T], client:ActorRef)

/**
 * Message used to send result values. Used internally and externally.
 */
case class OperationResult[T](result:CalculationResult[T], pos: Int)


/**
 * When run, Operations will return a StepOutput. This will either indicate a
 * complete result (StepResult), an error (StepError), or indicate that it
 * needs other work performed asynchronously before it can continue.
 */
sealed trait StepOutput[+T]

case class StepResult[T](value:T) extends StepOutput[T]
case class StepError(msg:String, trace:String) extends StepOutput[Nothing]
case class StepRequiresAsync[T](args:Args, cb:Callback[T]) extends StepOutput[T]

object StepError {
  def fromException(e:Throwable) = {
    val msg = e.getMessage
    val trace = e.getStackTrace.map(_.toString).mkString("\n")
    StepError(msg, trace)
  }
}

// TODO: refactor code that needs this, then remove it
object StepOutput {
  implicit def someToStepOutput[T](o:Some[T]) = StepResult(o.get)
}

trait TrellisActor extends Actor {
  def debug = false
  def log(msg:String) = if(debug) println(msg)
  def err(msg:String) = println(msg)
}

/**
 * Actor responsible for dispatching and executing operations.
 */
case class ServerActor(id: String, server: Server) extends TrellisActor {
  val dispatcher: ActorRef = context.actorOf(Props(Dispatcher(server)))

  def receive = {
    case Run(op) => {
      log("server asked to run op %s" format op)
      dispatcher ! RunOperation(op, 0, sender)
    }

    case RunCallback(args, pos, cb, client) => {
      log("server asked to run callback %s %s" format (args, cb))
      context.actorOf(Props(Calculation(server, pos, args, cb, client, dispatcher)))
    }

    case msg => sys.error("unknown message: %s" format msg)
  }
}

/**
 * Dispatcher is responsible for forwarding work to workers.
 */
case class Dispatcher(server: Server) extends TrellisActor {
  def receive = {
    case msg:RunOperation[_] => {
      log("dispatcher asked to run op")
      context.actorOf(Props(Worker(server))) ! msg
    }

    case r => sys.error("Dispatcher received unknown result.")
  }
}

trait WorkerLike extends TrellisActor {
  protected[this] var startTime:Long = 0L
  protected[this] var workStartTime:Long = 0L

  def server:Server
  def id:String = "myid"
  def getChildHistories():List[History]

  def handleResult[T](pos: Int, client: ActorRef, output: StepOutput[T]) {
    log("worker-like (%s) got output %d: %s" format (this, pos, output))

    output match {
      // ok, this operation completed and we have a value. so return it.
      case StepResult(value) => {
        log(" output was a result %s" format value)
        val history = Success(id, startTime, time(), getChildHistories())
        val result = OperationResult(Complete(value, history), pos)
        log(" sending %s back to client" format result)
        client ! result
        log(" sent")
      }

      // there was an error, so return that as well.
      case StepError(msg, trace) => {
        log(" output was an error %s" format msg)
        val history = Failure(id, startTime, time(), getChildHistories(), msg, trace)
        client ! OperationResult(Error(msg, history), pos)
      }

      // we need to do more work, so as the server to do it asynchronously.
      case StepRequiresAsync(args, cb) => {
        log(" output requires async: %s" format args.toList)
        server.actor ! RunCallback(args, pos, cb, client)
      }
    }
  }
}


case class Worker(val server: Server) extends WorkerLike {
  def getChildHistories():List[History] = Nil

  def receive = {
    case RunOperation(op, pos, client) => {
      log("worker: run operation (%d): %s" format (pos, op))
      try {
        handleResult(pos, client, op.run(server))
      } catch {
        case e => handleResult(pos, client, StepError.fromException(e))
      }
      context.stop(self)
    }
    case x => sys.error("worker got unknown msg: %s" format x)
  }
}

case class Calculation[T](val server:Server, pos:Int, args:Args,
                          cb:Callback[T], client:ActorRef, dispatcher:ActorRef)
extends WorkerLike {

  // results won't (necessarily) share any type info with each other, so we
  // have to use Any as the least-upper type bound :(
  val results = Array.ofDim[CalculationResult[Any]](args.length)

  // just after starting the actor, we need to dispatch out the child
  // operations to be run. if none of those existed, we should run the
  // callback and be done.
  override def preStart {
    for (i <- 0 until args.length) {
      log(" calculation looking at %d: %s" format (i, args(i)))
      args(i) match {
        case op:Operation[_] => dispatcher ! RunOperation(op, i, self)
        case value => results(i) = Inlined(value)
      }
    }

    if (isDone) finishCallback()
  }

  // 
  def getChildHistories() = results.toList.flatMap {
    case Complete(_, history) => Some(history)
    case Error(_, history) => Some(history)
    case Inlined(_) => None
  }

  // if any entry in the results array is null, we're not done.
  def isDone = results.find(_ == null).isEmpty

  // if any entry in the results array is Error, we have an error.
  def hasError = results.find(_.isInstanceOf[Error]).isDefined

  // create a list of the actual values of our children
  def getValues = results.toList.flatMap {
    case Complete(value, _) => Some(value)
    case Inlined(value) => Some(value)
    case r => sys.error("found unexpected result %s" format r)
  }

  // this is called when we have heard back from all our sub-operations and
  // are ready to begin evaluation. after this point we will terminate and not
  // receive any more messages.
  def finishCallback() {
    log(" all values complete")
    handleResult(pos, client, cb(getValues))

    log(" calculation done: performing callback")
    context.stop(self)
  }

  def receive = {
    case OperationResult(childResult,  pos) => {
      log("calculation got result %d" format pos)
      results(pos) = childResult

      if (!isDone) {
      } else if (hasError) {
        val h = Failure(id, startTime, time(), getChildHistories(), "child failed", "")
        client ! Error("this is a failure message", h)
      } else {
        log(" all values complete")
        finishCallback()
      }
    }

    case g => err("calculation got unknown message: %s" format g)
  }
}
