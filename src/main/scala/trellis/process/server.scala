package trellis.process

import java.io.File
import scala.util.matching.Regex

import trellis.data._
import trellis.data.FileExtensionRegexes._
import trellis.RasterExtent
import trellis.operation._
import trellis.IntRaster

// akka imports
import akka.actor._
import akka.routing._
import akka.dispatch.Await
import akka.util.duration._
import akka.util.Timeout

class Context (server:Server) {
  val timer = new Timer()

  def loadRaster(path:String, g:RasterExtent):IntRaster = {
    server.getRaster(path, None, Option(g))
  }

  def getRaster(path:String, layer:RasterLayer, re:RasterExtent):IntRaster = {
    server.getRaster(path, Option(layer), Option(re))
  }

  def getRasterByName(name:String, re:RasterExtent):IntRaster = {
    server.getRasterByName(name, Option(re))
  }

  def getRasterExtentByName(name:String):RasterExtent = {
    server.getRasterExtentByName(name)
  }
}

import com.typesafe.config.ConfigFactory

class Server (id:String, val catalog:Catalog) extends FileCaching {
  val debug = false

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

  var system:akka.actor.ActorSystem = akka.actor.ActorSystem(id, ConfigFactory.load())
  var actor:akka.actor.ActorRef = system.actorOf(Props(new ServerActor(id, this)), "server")

  def shutdown() { system.shutdown() }

  def log(msg:String) = if(debug) println(msg)

  def run[T:Manifest](op:Op[T]):T = getResult(op) match {
    case Complete(value, _) => value
    case Error(msg, trace) => sys.error(msg)
  }

  def getResult[T:Manifest](op:Op[T]) = _run(op)

  private[process] def _run[T:Manifest](op:Op[T]) = {
    log("server._run called with %s" format op)

    implicit val timeout = Timeout(60 seconds)

    val future = op match {
      case op:DispatchedOp[_] => (actor ? RunDispatched(op.op, op.dispatcher)).mapTo[OperationResult[T]]
      case op:Op[_]           => (actor ? Run(op)).mapTo[OperationResult[T]]
    }

    val result = Await.result(future, 60 seconds)

    result match {
      case OperationResult(c:Complete[_], _) => c.asInstanceOf[Complete[T]]
      case OperationResult(e:Error, _) => e
      case r => sys.error("unexpected status: %s" format r)
    }
  }
}

trait FileCaching {
  val mb = 1<<20

  // TODO: set this in config file
  val maxBytesInCache = 2000 * mb // 2G
  var caching = true //xyz
  var cacheSize:Int = 0
  val cache:CacheStrategy[String,Array[Byte]] = new MRUCache(maxBytesInCache, _.length)
  val rasterCache:HashBackedCache[String,IntRaster] = new MRUCache(maxBytesInCache, _.length * 4)

  val catalog:Catalog

  var id = "default"

  //// TODO: remove this and add cache configuration to server's JSON config
  ////       and/or constructor arguments
  //def enableCaching() { caching = true }
  //def disableCaching() { caching = false }

  def getRasterExtentByName(name:String):RasterExtent = {
    catalog.getRasterLayerByName(name) match {
      case Some((path, layer)) => layer.rasterExtent
      case None => sys.error("couldn't find %s" format name)
    }
  }

  def getRasterByName(name:String, reOpt:Option[RasterExtent]):IntRaster = {
    catalog.getRasterLayerByName(name) match {
      case Some((path, layer)) => getRaster(path, Option(layer), reOpt)
      case None => sys.error("couldn't find %s" format name)
    }
  }

  def loadRaster(path:String, g:RasterExtent):IntRaster = getRaster(path, None, Option(g))

  /**
   * THIS is the new thing that we are wanting to use.
   */
  def getRaster(path:String, layerOpt:Option[RasterLayer], reOpt:Option[RasterExtent]):IntRaster = {

    def xyz(path:String, layerOpt:Option[RasterLayer]) = {
      getReader(path).read(path, layerOpt, None)
    }

    if (this.caching) {
      val raster = rasterCache.getOrInsert(path, xyz(path, layerOpt))
      IntRasterReader.read(raster, reOpt)
    } else {
      getReader(path).read(path, layerOpt, reOpt)
    }
  }

  def recursiveListFiles(f: File, r: Regex): Array[File] = {
    val these = f.listFiles
    val good = these.filter(f => r.findFirstIn(f.getName).isDefined)
    good ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_,r))
  }

  def cacheRasters() = catalog.stores.foreach {
    case (name, store) => store.layers.values.foreach {
      case (path, layer) => getRaster(path, Some(layer), None)
    }
  }

  def getReader(path:String): FileReader = path match {
    case Arg32Pattern() => Arg32Reader
    case ArgPattern() => ArgReader
    case GeoTiffPattern() => GeoTiffReader
    case AsciiPattern() => AsciiReader
    case _ => sys.error("unknown path type %s".format(path))
  }
}

object Server {
  val config = ConfigFactory.load()
  val catalogPath = config.getString("trellis.catalog")

  def apply(id:String) = new Server(id, Catalog.fromPath(catalogPath))
  def apply(id:String, path:String) = new Server(id, Catalog.fromPath(path))
  def apply(id:String, catalog:Catalog) = new Server(id, catalog)
}

object TestServer {
  def apply() = Server("test", Catalog.empty("test"))
  def apply(path:String) = new Server("test", Catalog.fromPath(path))
}
