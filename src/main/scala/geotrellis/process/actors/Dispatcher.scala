package geotrellis.process.actors

import akka.actor._
import akka.routing._

import geotrellis._
import geotrellis.process._

/**
 * Dispatcher is responsible for forwarding work to workers.
 */
case class Dispatcher(server: Server) extends Actor {

  val pool = context.actorOf(Props(Worker(server)).withRouter(RoundRobinRouter( nrOfInstances = 120 )))

  // Actor event loop
  def receive = {
    case RunOperation(op,pos,client,dispatcher) => {
      op match {
        case RemoteOperation(sendOp, None) => 
          server.getRouter ! RunOperation(sendOp,pos,client,None)
        case RemoteOperation(sendOp, Some(cluster)) => 
          cluster ! RunOperation(sendOp,pos,client,None)
        case _ =>
          val outgoingDispatcher = dispatcher match {
            case None => Some(self)
            case _ => dispatcher
          }
          pool ! RunOperation(op,pos,client,outgoingDispatcher)
      }
    }
    case r => sys.error("Dispatcher received unknown result.")
  }
}
