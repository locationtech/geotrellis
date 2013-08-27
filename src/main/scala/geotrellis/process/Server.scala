package geotrellis.process

import java.io.File
import scala.util.matching.Regex
import scala.collection.mutable
import geotrellis._
import geotrellis._
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis.data.FileExtensionRegexes._
import geotrellis.RasterExtent
import geotrellis._
import geotrellis.util._
import akka.actor._
import akka.routing._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.util.Timeout
import akka.pattern.ask
import java.util.concurrent.TimeUnit
import com.typesafe.config.ConfigFactory
import scala.util.{Try,Success => TrySuccess, Failure => TryFailure}
import geotrellis.util.Filesystem

class Server (id:String, val catalog:Catalog) extends Serializable {
  val debug = false

  var actor:akka.actor.ActorRef = Server.actorSystem.actorOf(Props(new ServerActor(id, this)), id)

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

  def runSource[T:Manifest](src:DataSource[T,_]) = {
    run(src.get)
  }

  def run[T:Manifest](op:Op[T]):T = getResult(op) match {
    case Complete(value, _) => value
    case Error(msg, trace) => {
      println(s"Operation Error. Trace: $trace")
      sys.error(msg)
    }
  }

  def getResult[T:Manifest](op:Op[T]) = _run(op)

  private[process] def _run[T:Manifest](op:Op[T]) = {
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
  val config = ConfigFactory.load()
  def catalogPath = config.getString("geotrellis.catalog")

  def apply(id:String) = new Server(id, Catalog.fromPath(catalogPath))
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
