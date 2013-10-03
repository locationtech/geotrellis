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
    case RunOperation(op,pos,client,None) => pool ! RunOperation(op,pos,client,Some(self))
    case msg:RunOperation[_] => pool ! msg 
    case r => sys.error("Dispatcher received unknown result.")
  }
}
