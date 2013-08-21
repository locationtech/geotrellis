package geotrellis.process

// akka imports
import akka.actor._
import akka.routing._
import scala.concurrent.Await
import scala.concurrent.duration._

import geotrellis._

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


/*******************************************************
 * External messages sent from Trellis land (non-actors)
 *******************************************************
 */

/**
 * External message to compute the given operation and return result to sender.
 * Run child operations using default local dispatcher.
 */
case class Run(op:Operation[_])

/**
 * External message to compute the given operation and return result to sender.
 * Dispatch child operations with provided dispatcher.
 */
case class RunDispatched(op:Operation[_], dispatcher:ActorRef)


/********************
 * Internal messages 
 ********************
 */

/**
 * Internal message to run the provided op and send the result to the client.
 */
case class RunOperation[T](op: Operation[T], pos: Int, client: ActorRef, dispatcher:Option[ActorRef])

/**
 * Internal message to compute the provided args (if necessary), invoke the
 * provided callback with the computed args, and send the result to the client.
 */
case class RunCallback[T](args:Args, pos:Int, cb:Callback[T], client:ActorRef, id:String, dispatcher:ActorRef)

/**
 * Message used to send result values. Used internally and externally.
 */
case class OperationResult[T](result:CalculationResult[T], pos: Int)

/**
 * Actor responsible for dispatching and executing operations.
 *
 * This is a long-running actor which expects to receive two kinds of messages:
 * geotrellis.raster.TileSpec
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
    // EXTERNAL MESSAGES
    case Run(op) => {
      val msgSender = sender
      dispatcher ! RunOperation(op, 0, msgSender, None)
    }
 
    // internal message sent from external source (a remote server)
    case msg:RunOperation[_] => { 
      val s = sender

      dispatcher ! msg
    }

    case RunDispatched(op,childDispatcher) => {
      val msgSender = sender
      this.dispatcher ! RunOperation(op, 0, msgSender, Some(childDispatcher)) 
    }

    // INTERNAL MESSAGES
    case RunCallback(args, pos, cb, client, id, dispatcher) => {
      context.actorOf(Props(Calculation(server, pos, args, cb, client, dispatcher, id)))
    }

    case msg => sys.error("unknown message: %s" format msg)
  }
}

/**
 * Dispatcher is responsible for forwarding work to workers.
 */
case class Dispatcher(server: Server) extends Actor {

  val pool = context.actorOf(Props(Worker(server)).withRouter(RoundRobinRouter( nrOfInstances = 120 )))

  // Actor event loop
  def receive = {
    case RunOperation(op,pos,client,None) => pool ! RunOperation(op,pos,client,Some(self))
    case msg:RunOperation[_] => pool ! msg 
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
  def handleResult[T](pos:Int, client:ActorRef, output:StepOutput[T], t:Option[Timer], dispatcher:ActorRef) {

    output match {
      // ok, this operation completed and we have a value. so return it.
      case Result(value) => {

        val history = success(id, startTime, time(), t)
        val result = OperationResult(Complete(value, history), pos)

        client ! result
      }

      // Execute the returned operation as the next step of this calculation.
      case AndThen(op) => {
         server.actor ! RunOperation(op, pos, client, Some(dispatcher))
      }

      // there was an error, so return that as well.
      case StepError(msg, trace) => {
        val history = failure(id, startTime, time(), t, msg, trace)
        client ! OperationResult(Error(msg, history), pos)
      }

      // we need to do more work, so as the server to do it asynchronously.
      case StepRequiresAsync(args, cb) => {
        server.actor ! RunCallback(args, pos, cb, client, id, dispatcher)
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
case class Worker(val server: Server) extends WorkerLike {
  // Workers themselves don't have direct children. If the operation in
  // question has child operations it will be processed by a Calculation
  // instead, who will be responsible for constructing the response (including
  // history).

  private var _id = ""
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
    case RunOperation(incomingOp, pos, client, Some(ourDispatcher)) => {
      // Worker has received an operation to execute.
      startTime = time()
      // If the the children of this operation should be run remotely,
      // replace our dispatcher with the remote dispatcher.
      val (op, dispatcher) = incomingOp match {
        case DispatchedOperation(runOp, newDispatcher) => (runOp, newDispatcher)
        case _ => (incomingOp, ourDispatcher)
      }
      _id = op.name
      val geotrellisContext = new Context(server)
      try {
        val z = op.run(geotrellisContext)
        handleResult(pos, client, z, Some(geotrellisContext.timer), dispatcher)
      } catch {
        case e:Throwable => {
          val error = StepError.fromException(e)
          System.err.println("Operation failed, with exception: " + 
            s"${e}\n\nStack trace:\n${error.trace}\n", error.msg,error.trace)
          handleResult(pos, client, error, Some(geotrellisContext.timer), dispatcher)
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
case class Calculation[T](val server:Server, pos:Int, args:Args,
                          cb:Callback[T], client:ActorRef, dispatcher:ActorRef,
                          _id:String)
extends WorkerLike {

  def id = "calc " + _id

  startTime = time()

  // These results won't (necessarily) share any type info with each other, so
  // we have to use Any as the least-upper type bound :(
  val results = Array.fill[Option[CalculationResult[Any]]](args.length)(None)

  // Just after starting the actor, we need to dispatch out the child
  // operations to be run. If none of those existed, we should run the
  // callback and be done.
  override def preStart {
    println(s"in Calculation preStart for $id")
    for (i <- 0 until args.length) {
      args(i) match {
        case op:Operation[_] => {
          println(s"sending out op for execution: $op")
          dispatcher ! RunOperation(op, i, self, None)
        }
        case value => { 
        	println(s"Received inlined value (no execution): $value")
        	results(i) = Some(Inlined(value))
        }
      }
    }

    if (isDone) { 
      println("isDone right away")
      finishCallback()
      context.stop(self)
    }
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
      handleResult(pos, client, cb(getValues), None, dispatcher)
    } catch {
      case e:Throwable => {
        val error = StepError.fromException(e)
        System.err.println(s"Operation failed, with exception: $e\n\nStack trace:${error.trace}\n\n")
        handleResult(pos, client, error, None, dispatcher)
      }
    }
    context.stop(self)
  }

  // Actor event loop
  def receive = {
    case OperationResult(childResult,  pos) => {
      println(s"received result $childResult FOR PARENT ${_id} AT POSITION $pos")
      results(pos) = Some(childResult)
      if (!isDone) {
      } else if (hasError) {
        val se = StepError("error", "error")
        handleResult(this.pos, client, se , None, dispatcher)
        context.stop(self)
      } else {
        println(s"all child operations of ${_id} have completed.  invoking callback")
        finishCallback()
        context.stop(self)
      }
    }

    case g => sys.error("calculation got unknown message: %s" format g)
  }
}
