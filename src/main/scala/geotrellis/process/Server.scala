package geotrellis.process

import geotrellis._
import geotrellis.process.actors._

import akka.actor._
import akka.pattern.ask

import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import geotrellis.source.DataSource

class Server (id:String, val catalog:Catalog) extends Serializable {
  val debug = false

  var actor:akka.actor.ActorRef = Server.actorSystem.actorOf(Props(new ServerActor(this)), id)

  private[this] val cache = new HashCache()

  catalog.initCache(cache)

  Server.startActorSystem
  def system = Server.actorSystem

  def startUp:Unit = ()

  def shutdown():Unit = { 
    Server.actorSystem.shutdown()
    Server.actorSystem.awaitTermination()
  }

  def log(msg:String) = if(debug) println(msg)

  def runSource[T:Manifest](src:DataSource[_,T]):T =
    run(src.get)

  def getSource[T:Manifest](src:DataSource[_,T]):CalculationResult[T] =
    getResult(src.get)
  
  def run[T:Manifest](op:Op[T]):T = 
    getResult(op) match {
      case Complete(value, _) => value
      case Error(msg, trace) =>
        println(s"Operation Error. Trace: $trace")
        sys.error(msg)
    }

  def getResult[T:Manifest](op:Op[T]):CalculationResult[T] = _run(op)

  private[process] def _run[T:Manifest](op:Op[T]):CalculationResult[T] = {
    log("server._run called with %s" format op)

    val d = Duration.create(60000, TimeUnit.SECONDS)
    implicit val t = Timeout(d)
    val future = op match {
      case op:DispatchedOperation[_] => 
        (actor ? RunDispatched(op.op, op.dispatcher)).mapTo[OperationResult[T]]
      case op:Op[_]           => (actor ? Run(op)).mapTo[OperationResult[T]]
    }

    val result = Await.result(future, d)

    result match {
      case OperationResult(c:Complete[_], _) => c.asInstanceOf[Complete[T]]
      case OperationResult(e:Error, _) => e
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
