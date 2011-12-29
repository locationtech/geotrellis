package trellis.process

import java.io.{File,FileInputStream}
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel.MapMode._
//import scala.actors.{Future, Actor}
//import scala.actors.Actor._
//import scala.actors.OutputChannel
import scala.collection.mutable.{HashMap, ArrayBuffer,ListBuffer,Map=>MMap}
import scala.util.matching.Regex
import trellis.data._
import trellis.data.FileExtensionRegexes._

import trellis.RasterExtent
import trellis.operation._
import trellis.raster.IntRaster
import scala.math.min

// akka imports
import akka.actor._
import akka.routing._
import akka.dispatch.Await
import akka.util.duration._

import sys.error

class Server (id:String, val catalog:Catalog) extends FileCaching {
  val system = akka.actor.ActorSystem(id)
  val actor = system.actorOf(Props(new ServerActor(id, this)), "server")
  
  def run[T:Manifest] (op:Operation[T]):T = {

    implicit val timeout = system.settings.ActorTimeout
    val future = (actor ? Run(op)).mapTo[CalculationResult[T]]
    val result = Await.result(future, 5 seconds)
    val r = result match {
      //TODO: eventually calculation result shouldn't be a list
      case Complete(r :: Nil) => r.asInstanceOf[T]
      case Error(msg) => error("server(%s) returned an error: %s".format(op,msg))
      case _ => error("unexpected status: %s" format (result) )
    }
    r
  }
}

trait FileCaching {
  val mb = math.pow(2,20).toLong
  val maxBytesInCache = 2000 * mb
  val cache:CacheStrategy[String,Array[Byte]] = new MRUCache(maxBytesInCache, _.length)
  val rasterCache:HashBackedCache[String,IntRaster] = new MRUCache(maxBytesInCache, _.length * 4)

  val catalog:Catalog

  var cacheSize:Int = 0
  val maxCacheSize:Int = 1000000000 

  var caching = false

  var id = "default"

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
