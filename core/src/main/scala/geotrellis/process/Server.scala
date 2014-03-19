/*
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
 */

package geotrellis.process

import geotrellis._
import geotrellis.process.actors._

import akka.actor._
import akka.pattern.ask
import akka.routing.FromConfig

import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import geotrellis.source.DataSource

import scala.collection.mutable

class Server (id:String, val catalog:Catalog) extends Serializable {
  val debug = false

  private[process] val layerLoader = new LayerLoader(this)

  private[this] val cache = new HashCache[String]()

  catalog.initCache(cache)

  var actor:akka.actor.ActorRef = Server.actorSystem.actorOf(Props(classOf[ServerActor],this), id)

  Server.startActorSystem
  def system = Server.actorSystem

  def startUp:Unit = ()

  def shutdown():Unit = { 
    Server.actorSystem.shutdown()
    Server.actorSystem.awaitTermination()
  }

  private val routers = mutable.Map[String,ActorRef]()
  def getRouter():ActorRef = getRouter("clusterRouter")
  def getRouter(routerName:String):ActorRef = {
    if(!routers.contains(routerName)) { 
      routers(routerName) = 
        system.actorOf(
          Props.empty.withRouter(FromConfig),
          name = routerName)
    }
    routers(routerName)
  }

  def log(msg:String) = if(debug) println(msg)

  def get[T](src:DataSource[_,T]):T =
    run(src) match {
      case Complete(value, _) => value
      case Error(msg, trace) =>
        println(s"Operation Error. Trace: $trace")
        sys.error(msg)
    }
  
  def get[T](op:Op[T]):T = 
    run(op) match {
      case Complete(value, _) => value
      case Error(msg, trace) =>
        println(s"Operation Error. Trace: $trace")
        sys.error(msg)
    }

  def run[T](src:DataSource[_,T]):OperationResult[T] =
    run(src.convergeOp)

  def run[T](op:Op[T]):OperationResult[T] = 
    _run(op)

  private[process] def _run[T](op:Op[T]):OperationResult[T] = {
    log("server._run called with %s" format op)

    val d = Duration.create(60000, TimeUnit.SECONDS)
    implicit val t = Timeout(d)
    val future = 
        (actor ? Run(op)).mapTo[PositionedResult[T]]

    val result = Await.result(future, d)

    result match {
      case PositionedResult(c:Complete[_], _) => c.asInstanceOf[Complete[T]]
      case PositionedResult(e:Error, _) => e
      case r => sys.error("unexpected status: %s" format r)
    }
  }
}

object Server {
  def apply(id:String, path:String) = new Server(id, Catalog.fromPath(path))
  def apply(id:String, catalog:Catalog) = new Server(id, catalog)
  def empty(id:String) = new Server(id, Catalog.empty(id))

  var actorSystem:akka.actor.ActorSystem = akka.actor.ActorSystem("GeoTrellis", ConfigFactory.load())

  def startActorSystem {
    if (actorSystem.isTerminated) {
      actorSystem = akka.actor.ActorSystem("GeoTrellis", ConfigFactory.load())
    } 
  }
}
