/*******************************************************************************
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package geotrellis.process.actors

import geotrellis._
import geotrellis.process._
import akka.actor._
import akka.routing._

case class ServerContext(externalId:String,layerLoader:LayerLoader,serverRef:ActorRef)

/**
 * Actor responsible for dispatching and executing operations.
 *
 * This is a long-running actor which expects to receive two kinds of messages:
 *  1. Requests made by the outside world to run operations.
 *  2. Requests made by other actors to asynchronously evaluate arguments.
 *
 * In the first case, we dispatch the message to a pool of workers). In the second 
 * case we will spin up a Step Aggregator actor who will handle the message.
 */
case class ServerActor(server: Server) extends Actor {
  val fullExternalId = context.system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress.toString
  val externalId = if (fullExternalId.startsWith("akka.tcp://GeoTrellis@")) 
    fullExternalId.substring(22)
  else 
    ""

  val serverContext = ServerContext(externalId, server.layerLoader,self)

  val workerPool = context.actorOf(Props(Worker(serverContext)).withRouter(RoundRobinRouter( nrOfInstances = 120 )))

  // Actor event loop
  def receive = {
    case Run(op) =>
      val msgSender = sender
      workerPool ! RunOperation(op, 0, msgSender)
 
    case RunOperation(op,pos,client) => 
      op match {
        case Literal(value) => 
          val hist = History(op,serverContext.externalId).withResult(value)
          client ! PositionedResult(Complete(value, hist.withResult(value).withResult(value)), pos)
        case RemoteOperation(sendOp, None) => 
          server.getRouter ! RunOperation(sendOp,pos,client)
        case RemoteOperation(sendOp, Some(cluster)) => 
          cluster ! RunOperation(sendOp,pos,client)
        case _ =>
          workerPool ! RunOperation(op,pos,client)
      }
    
    case RunCallback(args, pos, cb, client, tracker) =>
      context.actorOf(Props(StepAggregator(serverContext, pos, args, cb, client, tracker)))

    case msg => sys.error("ServerActor recieved unknown message: %s" format msg)
  }
}
