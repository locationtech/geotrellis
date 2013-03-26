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

//class Server (id:String, val catalog:Catalog) extends FileCaching {
class Server (id:String, val catalog:Catalog) {
  val debug = false

  var system:akka.actor.ActorSystem = akka.actor.ActorSystem(id, ConfigFactory.load())
  var actor:akka.actor.ActorRef = system.actorOf(Props(new ServerActor(id, this)), "server")

  private[this] val staticCache = mutable.Map.empty[String, Array[Byte]]

  val customConf2 = ConfigFactory.parseString("""
akka {
  version = "2.0-M2"
  logConfigOnStart = off
  loglevel = "INFO"
  stdout-loglevel = "INFO"
  event-handlers = ["akka.event.Logging$DefaultLogger"]
  remote {
    client {
      "message-frame-size": "100 MiB"
    },
    server {
      "message-frame-size": "100 MiB"
    }
  }
  default-dispatcher {
    core-pool-size-factor = 80.0
  }
  actor {
    debug {
      receive = off
      autoreceive = off
      lifecycle = off
    }
    deployment {
      /routey {
        nr-of-instances = 300
      }
    }
  }
}
""")

  initStaticCache()

  def startUp:Unit = ()

  def initStaticCache():Unit = {
    val cacheStores = catalog.stores.values.filter(_.hasCacheAll)
    cacheStores.foreach(_.getLayers.foreach(l => loadInStaticCache(l)))

    val n = staticCache.size
    val (amt, units) = Units.bytes(staticCache.foldLeft(0)(_ + _._2.length))
    log("loaded %d layers (%.2f %s) into static cache" format (n, amt, units))
  }

  def loadInStaticCache(layer:RasterLayer):Unit = {
    val path = layer.rasterPath
    if (new File(path).exists) {
      staticCache(layer.name) = Filesystem.slurp(path)
    }
  }

  def shutdown():Unit = system.shutdown()

  def log(msg:String) = if(debug) println(msg)

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

//    implicit val timeout = Timeout(Duration(3600, "millis"))
    val d = Duration.create(600, TimeUnit.SECONDS)
    implicit val t = Timeout(d)
    val future = op match {
      case op:DispatchedOperation[_] => (actor ? RunDispatched(op.op, op.dispatcher)).mapTo[OperationResult[T]]
      case op:Op[_]           => (actor ? Run(op)).mapTo[OperationResult[T]]
    }

    val result = Await.result(future, d)

    result match {
      case OperationResult(c:Complete[_], _) => c.asInstanceOf[Complete[T]]
      case OperationResult(e:Error, _) => e
      case r => sys.error("unexpected status: %s" format r)
    }
  }

  def metadataPath(path:String) = {
    path.substring(0, path.lastIndexOf(".")) + ".json"
  }

  /**
   * Return the appropriate reader object for the given path.
   */
  def getReader(path:String, layerOpt:Option[RasterLayer]): FileReader = {
    path match {
      case ArgPattern() => ArgReader
      case AsciiPattern() => AsciiReader
      case _ => sys.error("unknown path type %s".format(path))
    }
  }
   
  def getRasterStepOutput(path:String, layerOpt:Option[RasterLayer], reOpt:Option[RasterExtent]) = {
    val reader = getReader(path, layerOpt)
    Try(reader.readPath(path, layerOpt, reOpt)) match { 
      case TrySuccess(r) => Result(r)
      case TryFailure(e) =>
        StepError(s"Could not load raster from path: ${path}. Reason: $e","")
    }
  }

  def getRaster(path:String, layerOpt:Option[RasterLayer], reOpt:Option[RasterExtent]):Raster = {
    getReader(path, layerOpt).readPath(path, layerOpt, reOpt)  
  }

  // TODO: rewrite calls to loadRaster to getRaster. then remove?
  def loadRaster(path:String):Raster = getRaster(path, None, None)
  def loadRaster(path:String, g:RasterExtent):Raster = getRaster(path, None, Option(g))

  def getRasterExtentByName(name:String):RasterExtent = {
    catalog.getRasterLayerByName(name) match {
      case Some(layer) => layer.rasterExtent
      case None => sys.error("couldn't find %s" format name)
    }
  }

  def getRasterByName(name:String, reOpt:Option[RasterExtent]):StepOutput[Raster] = {
    catalog.getRasterLayerByName(name) match {
      case Some(layer) => {
        val path = layer.rasterPath
        val reader = getReader(path, Some(layer))
        val r = staticCache.get(layer.name) match {
          case Some(bytes) => reader.readCache(bytes, layer, reOpt)
          case None => reader.readPath(path, Some(layer), reOpt)
        }
        Result(r)
      }
      case None => {
        val debugInfo = "Failed to load raster ${name} from catalog at ${catalog.source}" + 
          " with json: \n" + catalog.json
        StepError(s"Did not find raster '${name}' in catalog", debugInfo)
      }
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
}


