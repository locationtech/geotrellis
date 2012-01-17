package trellis.process

// akka imports
import akka.actor._
import akka.routing._
import akka.dispatch.Await
import akka.util.duration._

import trellis.operation.Operation

/**
 * CalculationResult contains an operation's results.
 *
 * This could include the resulting value the operation produced, an error
 * that prevented the operation from completing, and the history of the
 * operation.
 */
sealed trait CalculationResult[+T]

/**
 * CalculationResult for an operation which was a literal argument.
 *
 * Instances of Inlined should never leak out of the actor world. E.g. messages
 * sent to clients in the Trellis world should either be Complete or Failure.
 *
 * Inlined exists because these arguments don't have useful history, and
 * Calculations need to distinguish them from Complete results (which were
 * calculated operations with history).
 */
case class Inlined[T](value:T) extends CalculationResult[T]

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
case class RunCallback[T](args:Args, pos:Int, cb:Callback[T], client:ActorRef, id:String)

/**
 * Message used to send result values. Used internally and externally.
 */
case class OperationResult[T](result:CalculationResult[T], pos: Int)


/**
 * When run, Operations will return a StepOutput. This will either indicate a
 * complete result (Result), an error (StepError), or indicate that it
 * needs other work performed asynchronously before it can continue.
 */
sealed trait StepOutput[+T]

case class Result[T](value:T) extends StepOutput[T]
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
 * Actor responsible for dispatching and executing operations.
 *
 * This is a long-running actor which expects to receive two kinds of messages:
 * trellis.raster.TileSpec
 *  1. Requests made by the outside world to run operations.
 *  2. Requests made by other actors to asynchronously evaluate arguments.
 *
 * In the first case, we dispatch the message to Dispatcher (who is expected to
 * send the message to a workers). In the second case we will spin up a
 * Calculation actor who will handle the message.
 */
case class ServerActor(id: String, server: Server) extends Actor {
  val dispatcher: ActorRef = context.actorOf(Props(Dispatcher(server)))

  // Actor event loop
  def receive = {
    case Run(op) => {
      log("server asked to run op %s" format op)
      dispatcher ! RunOperation(op, 0, sender)
    }

    case RunCallback(args, pos, cb, client, id) => {
      log("server asked to run callback %s %s" format (args, cb))
      context.actorOf(Props(Calculation(server, pos, args, cb, client, dispatcher, id)))
    }

    case msg => sys.error("unknown message: %s" format msg)
  }
}

/**
 * Dispatcher is responsible for forwarding work to workers.
 */
case class Dispatcher(server: Server) extends Actor {

  // Actor event loop
  def receive = {
    case msg:RunOperation[_] => {
      log("dispatcher asked to run op")
      context.actorOf(Props(Worker(server))) ! msg
    }

    case r => sys.error("Dispatcher received unknown result.")
  }
}


/**
 * This trait contains functionality shared by Worker and Calculation.
 *
 * Mostly, this pertains to evaluating StepOutput, constructing
 * OperationResults and sending them back to the client.
 */
trait WorkerLike extends Actor {
  protected[this] var startTime:Long = 0L
  protected[this] var workStartTime:Long = 0L

  def server:Server

  def id:String 

  def success(id:String, start:Long, stop:Long, t:Option[Timer]): Success
  def failure(id:String, start:Long, stop:Long, t:Option[Timer], msg:String, trace:String): Failure

  // This method handles a given output. It will either return a result/error
  // to the client, or dispatch more asynchronous requests, as necessary.
  def handleResult[T](pos:Int, client:ActorRef, output:StepOutput[T], t:Option[Timer]) {
    log("worker-like (%s) got output %d: %s" format (this, pos, output))

    output match {
      // ok, this operation completed and we have a value. so return it.
      case Result(value) => {

        log(" output was a result %s" format value)
        val history = success(id, startTime, time(), t)
        log("&&& generated history: %s" format history)
        val result = OperationResult(Complete(value, history), pos)

        log(" sending %s back to client" format result)
        client ! result
        log(" sent")
      }

      // there was an error, so return that as well.
      case StepError(msg, trace) => {
        log(" output was an error %s" format msg)
        val history = failure(id, startTime, time(), t, msg, trace)
        client ! OperationResult(Error(msg, history), pos)
      }

      // we need to do more work, so as the server to do it asynchronously.
      case StepRequiresAsync(args, cb) => {
        log(" output requires async: %s" format args.toList)
        server.actor ! RunCallback(args, pos, cb, client, id)
      }
    }
  }
}


/**
 * Workers are responsible for evaluating an operation. However, if the
 * operation in question requires asynchronous callbacks, the work will be
 * off-loaded to a Calculation.
 *
 * Thus, in practice workers only ever do work on SimpleOperations.
 */
case class Worker(val server: Server) extends WorkerLike {
  // Workers themselves don't have direct children. If the operation in
  // question has child operations it will be processed by a Calculation
  // instead, who will be responsible for constructing the response (including
  // history).

  private var _id = ""
  //def id = "worker " + _id
  def id = _id

  def success(id:String, start:Long, stop:Long, t:Option[Timer]) = t match {
    case Some(timer) => timer.toSuccess(id, start, stop)
    case None => Success(id, start, stop, Nil)
  }

  def failure(id:String, start:Long, stop:Long, t:Option[Timer], msg:String, trace:String) = t match {
    case Some(timer) => timer.toFailure(id, start, stop, msg, trace)
    case None => Failure(id, start, stop, Nil, msg, trace)
  }

  // Actor event loop
  def receive = {
    case RunOperation(op, pos, client) => {
      //_id = op.toString
      _id = op.name
      startTime = time()
      log("worker: run operation (%d): %s" format (pos, op))
      //val timer = new Timer()
      val trellisContext = new Context(server)
      try {
        //val z = op.run(server)(timer)
        //handleResult(pos, client, z, Some(timer))
        val z = op.run(trellisContext)
        handleResult(pos, client, z, Some(trellisContext.timer))
      } catch {
        case e => {
          val error = StepError.fromException(e)
          handleResult(pos, client, error, Some(trellisContext.timer))
        }
      }
      context.stop(self)
    }
    case x => sys.error("worker got unknown msg: %s" format x)
  }
}

case class Calculation[T](val server:Server, pos:Int, args:Args,
                          cb:Callback[T], client:ActorRef, dispatcher:ActorRef,
                          _id:String)
extends WorkerLike {

  //def id = "calc " + _id
  def id = _id

  startTime = time()

  // These results won't (necessarily) share any type info with each other, so
  // we have to use Any as the least-upper type bound :(
  val results = Array.fill[Option[CalculationResult[Any]]](args.length)(None)

  // Just after starting the actor, we need to dispatch out the child
  // operations to be run. If none of those existed, we should run the
  // callback and be done.
  override def preStart {
    //startTime = time()
    for (i <- 0 until args.length) {
      log(" calculation looking at %d: %s" format (i, args(i)))
      args(i) match {
        case op:Operation[_] => dispatcher ! RunOperation(op, i, self)
        case value => results(i) = Some(Inlined(value))
      }
    }

    if (isDone) finishCallback()
  }

  // This should create a list of all the (non-trivial) child histories we
  // have. This leaves out inlined arguments, who don't have history in any
  // real sense (e.g. they were complete when we received them).
  def childHistories = results.toList.flatMap {
    case Some(Complete(_, history)) => Some(history)
    case Some(Error(_, history)) => Some(history)
    case Some(Inlined(_)) => None
    case None => None
  }

  def success(id:String, start:Long, stop:Long, t:Option[Timer]) = t match {
    case Some(timer) => Success(id, start, stop, childHistories ++ timer.children)
    case None => Success(id, start, stop, childHistories)
  }

  def failure(id:String, start:Long, stop:Long, t:Option[Timer], msg:String, trace:String) = t match {
    case Some(timer) => Failure(id, start, stop, childHistories ++ timer.children, msg, trace)
    case None => Failure(id, start, stop, childHistories, msg, trace)
  }

  // If any entry in the results array is null, we're not done.
  def isDone = results.find(_ == None).isEmpty

  // If any entry in the results array is Error, we have an error.
  def hasError = results.find { case Some(Error(_,_)) => true; case a => false } isDefined

  // Create a list of the actual values of our children.
  def getValues = results.toList.map {
    case Some(Complete(value, _)) => value
    case Some(Inlined(value)) => value
    case r => sys.error("found unexpected result %s" format r)
  }

  // This is called when we have heard back from all our sub-operations and
  // are ready to begin evaluation. After this point we will terminate and not
  // receive any more messages.
  def finishCallback() {
    log(" all values complete")
    handleResult(pos, client, cb(getValues), None)

    log(" calculation done: performing callback")
    context.stop(self)
  }

  // Actor event loop
  def receive = {
    case OperationResult(childResult,  pos) => {
      log("calculation got result %d" format pos)
      results(pos) = Some(childResult)

      if (!isDone) {
      } else if (hasError) {
        val h = failure(id, startTime, time(), None, "child failed", "")
        client ! Error("this is a failure message", h)
      } else {
        log(" all values complete")
        finishCallback()
      }
    }

    case g => sys.error("calculation got unknown message: %s" format g)
  }
}
