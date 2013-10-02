package geotrellis.process.actors

import geotrellis.process._
import akka.actor._

/**
 * Actor responsible for dispatching and executing operations.
 *
 * This is a long-running actor which expects to receive two kinds of messages:
 *  1. Requests made by the outside world to run operations.
 *  2. Requests made by other actors to asynchronously evaluate arguments.
 *
 * In the first case, we dispatch the message to Dispatcher (who is expected to
 * send the message to a workers). In the second case we will spin up a
 * Calculation actor who will handle the message.
 */
case class ServerActor(server: Server) extends Actor {
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
      dispatcher ! msg
    }

    case RunDispatched(op,childDispatcher) => {
      val msgSender = sender
      this.dispatcher ! RunOperation(op, 0, msgSender, Some(childDispatcher)) 
    }

    // INTERNAL MESSAGES
    case RunCallback(args, pos, cb, client, dispatcher,tracker) => {
      context.actorOf(Props(Calculation(server, pos, args, cb, client, dispatcher, tracker)))
    }

    case msg => sys.error("unknown message: %s" format msg)
  }
}
