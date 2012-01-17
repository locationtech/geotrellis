package trellis.process

import java.io.File
import scala.util.matching.Regex

import trellis.data._
import trellis.data.FileExtensionRegexes._
import trellis.RasterExtent
import trellis.operation._
import trellis.raster.IntRaster

// akka imports
import akka.actor._
import akka.routing._
import akka.dispatch.Await
import akka.util.duration._

class Context (server:Server) {
  val timer = new Timer()

  def run[T](op:Operation[T])(implicit m:Manifest[T]):T = {
    server._run(op)(m, timer).value
  }

  def getResult[T](op:Operation[T])(implicit m:Manifest[T]):Complete[T] = {
    server._run(op)(m, timer)
  }

  def getRaster(path:String, layer:RasterLayer, re:RasterExtent):IntRaster = {
    server.getRaster(path, layer, re)
  }

  def loadRaster(path:String, g:RasterExtent):IntRaster = {
    server.getRaster(path, null, g)
  }
}

class Server (id:String, val catalog:Catalog) extends FileCaching {
  val debug = false

  val system = akka.actor.ActorSystem(id)
  val actor = system.actorOf(Props(new ServerActor(id, this)), "server")

  def log(msg:String) = if(debug) println(msg)

  def run[T](op:Operation[T])(implicit m:Manifest[T]):T = {
    val context = new Context(this)
    context.run(op)(m)
  }

  def getResult[T](op:Operation[T])(implicit m:Manifest[T]):Complete[T] = {
    val context = new Context(this)
    context.getResult(op)(m)
  }

  private[process] def _run[T](op:Operation[T])(implicit m:Manifest[T], t:TimerLike):Complete[T] = {
    log("server.run called with %s" format op)

    implicit val timeout = system.settings.ActorTimeout
    val future = (actor ? Run(op)).mapTo[OperationResult[T]]
    val result = Await.result(future, 5 seconds)

    val result2:Complete[T] = result match {
      case OperationResult(c:Complete[_], _) => {
        val r = c.value
        val h = c.history
        log(" run is complete: received: %s" format r)
        log(op.toString)
        log("%s" format h.toPretty())
        t.add(h)
        //r.asInstanceOf[T]
        c.asInstanceOf[Complete[T]]
      }
      case OperationResult(Inlined(_), _) => {
        sys.error("server.run(%s) unexpected response: %s".format(op, result))
      }
      case OperationResult(Error(msg, trace), _) => {
        sys.error("server.run(%s) error: %s, trace: %s".format(op, msg, trace))
      }
      case _ => sys.error("unexpected status: %s" format result)
    }
    result2
  }
}

trait FileCaching {
  val mb = math.pow(2,20).toLong
  val maxBytesInCache = 2000 * mb
  val cache:CacheStrategy[String,Array[Byte]] = new MRUCache(maxBytesInCache, _.length)
  val rasterCache:HashBackedCache[String,IntRaster] = new MRUCache(maxBytesInCache, _.length * 4)

  val catalog:Catalog

  var cacheSize:Int = 0
  val maxCacheSize:Int = 1000 * 1000 * 1000 // 1G

  var caching = false

  var id = "default"

  // TODO: remove this and add cache configuration to server's JSON config
  //       and/or constructor arguments
  def enableCaching() { caching = true }
  def disableCaching() { caching = false }

  def loadRaster(path:String, g:RasterExtent):IntRaster = getRaster(path, null, g)

  /**
   * THIS is the new thing that we are wanting to use.
   */
  def getRaster(path:String, layer:RasterLayer, re:RasterExtent):IntRaster = {

    def xyz(path:String, layer:RasterLayer) = {
      getReader(path).read(path, Option(layer), None)
    }

    if (this.caching) {
      val raster = rasterCache.getOrInsert(path, xyz(path, layer))
      IntRasterReader.read(raster, Option(re))
    } else {
      getReader(path).read(path, Option(layer), Option(re))
    }
  }

  def recursiveListFiles(f: File, r: Regex): Array[File] = {
    val these = f.listFiles
    val good = these.filter(f => r.findFirstIn(f.getName).isDefined)
    good ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_,r))
  }

  def cacheRasters() = catalog.stores.foreach {
    case (name, store) => store.layers.values.foreach {
      case (path, layer) => getRaster(path, layer, null)
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
  val catalogPath = "/etc/trellis/catalog.json"

  def apply(id:String): Server = Server(id, Catalog.fromPath(catalogPath))

  def apply(id:String, catalog:Catalog): Server = {
    val server = new Server(id, catalog)
    server
  }
}

object TestServer {
  def apply() = Server("test", Catalog.empty("test"))
}
