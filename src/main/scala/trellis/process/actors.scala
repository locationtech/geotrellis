package trellis.process

// akka imports
import akka.actor._
import akka.routing._
import akka.dispatch.Await
import akka.util.duration._
import trellis.operation.Operation
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{ Map => MMap }

object Process {
  type ResultCallback = (CalculationResult[_]) => Any
}

import Process.ResultCallback

/*
 * Messages sent by Server-related actors
 */

/**
 * External messages sent to Trellis land (non-actors)
 */

//case class CalculationResult[T](value:Option[T])

/**
 * 
 */
sealed trait CalculationResult[+T]

case class Complete[T](value:T) extends CalculationResult[T]
case class Error(message:String) extends CalculationResult[Nothing]
case class InProgress() extends CalculationResult[Nothing]


/**
 * External messages sent from Trellis land (non-actors)
 */

/**
 * Asynchronously compute the provided operations (if any) and then invoke callback. 
 */
case class RunAsync(args:List[Any], callback: ResultCallback)

/**
 * Compute the operation and then invoke callback.
 */
case class Run(op:Operation[_])

/**
 * Message: Send to server to create new task group.
 */
//TODO: Replace Option[Results] with new result object
case class CreateTaskGroup(args: List[Any], callback: ResultCallback, dispatcher: ActorRef)

/**
 * Message: Send to server or dispatcher to run an operation.
 */
//TODO: remove gid, if we can get good id info from client ActorRef
//TODO: client is a taskgroup
case class RunOperation[T](op: Operation[T], position: Int, client: ActorRef)

/**
 * Message: The result of an individual operation.
 */
case class OperationResult[T](result: T, position: Int)

/**
 * Results from a task group; can include literal values that are passed through.
 * Used to start steps of an Operation, and to return the final result.
 */
case class Results(results: List[Any])

/**
 * Actor responsible for dispatching and executing operations.
 */
class ServerActor(id: String, val server: Server) extends Actor {

  var debug = false

  val dispatcher: ActorRef = context.actorOf(Props(new Dispatcher(server)))

  def receive = {
    case Run(op) => {
      val cb = makeCallback(sender)
      context.actorOf(Props(new Calculation(List(op), cb, dispatcher))) 
    }

    case RunAsync(args, cb) => {
      context.actorOf(Props(new Calculation(args, cb, dispatcher)))
    }

    case msg => {
      println("Received unknown message: %s".format(msg.toString()))
      context.stop(self)
    }
  }

  /**
   * Return value to non-actor client who requested calculation.
   */
  def makeCallback(client: ActorRef) = {
    (result:CalculationResult[_]) => {
      if (debug) println("performing server callback: sending message to client")
      client ! result
    }
  }
}

class Worker(server: Server) extends Actor {
  val debug = false
  def receive = {
    case RunOperation(op, position, client) => {
      if (debug) printf("  Worker: Received operation (%d) to execute: %s\n\n", position, op)
      try {
        op.run(server, sendResult(position, client, _: Any))
      } catch {
        case e: Exception => {
          e.printStackTrace()
          //TODO: replace w/ result object w/ error state
          sendResult(position, client, None)
        }
      }
      context.stop(self)
    }
    case x: Any => throw new Exception("Worker received unknown operation: %s".format(x.toString()))
  }

  def sendResult(position: Int, client: ActorRef, result: Any) = {
    if (debug) printf("  Worker (%s): Returning result of operation (%d): %s\n\n", this, position, result)
    client ! OperationResult(result, position)
  }
}

/**
 * Dispatcher is responsible for forwarding work to workers.
 */
class Dispatcher(server: Server) extends Actor {
  var debug = true

  def receive = {
    // override to perform load balancing, etc.
    case msg:RunOperation[_] => {
      val worker = context.actorOf(Props(new Worker(server)))
      worker ! msg
    }

    case r => {
      println("Dispatcher received unknown result.")
    }
  }
}

class Calculation(args: List[Any], callback: (CalculationResult[_]) => Any, dispatcher:ActorRef) extends Actor {
  val debug = false
  type OpTuple = (Int, Operation[_])

  var (results: Array[Any], ops: List[_]) = initialize(args)

  //TODO: refactor _results and _ops?
  def initialize(args: List[Any]) = {
    val _results = Array.fill[Any](args.length)('waiting)
    val _ops = ListBuffer[OpTuple]()

    for (i <- 0 until args.length) {
      args(i) match {
        case op: Operation[_] => { _ops.append((i, op)) }
        case other            => { _results(i) = other }
      }
    }
    (_results, _ops.toList)
  }
  
  override def preStart {
    if (ops.length == 0) {
      callback(Complete(results.toList))
    } else {
      ops.foreach {
        case (i: Int, op: Operation[_]) => {
          dispatcher ! RunOperation(op, i, self)
        }
      }
    }
   
  }
  def receive = {

    //TODO: Replace this with result message w/ error status
    case OperationResult(None, _) => callback(Error("task group received a result w/ None"))

    case OperationResult(result: Option[_],  position: Int) => {
      if (debug) println("Calculation received result")
      results(position) = result.get
      val done = // false if any results == None
        results.foldLeft (true) { (done, v) => done && v != 'waiting }
      //TODO: clear out internal state method
      if (done) {
        if (debug) println("taskgroup complete: performing callback.")
        callback(Complete(results.toList))
        results = null
        ops = null
        context.stop(self)
      }
    }

    case g => { println(" * * TaskGroup received unknown message." + g.toString()) }
  }
}

//TODO: type alias (maybe broader) for callback?
object Calculation {
  def apply(args: List[Any], callback: (CalculationResult[_]) => Any, dispatcher:ActorRef) 
    = new Calculation(args, callback, dispatcher)
}
