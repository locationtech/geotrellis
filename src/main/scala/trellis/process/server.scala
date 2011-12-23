package trellis.process

import java.io.{File,FileInputStream}
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel.MapMode._
import scala.actors.{Future, Actor}
import scala.actors.Actor._

import scala.actors.OutputChannel
import scala.collection.mutable.{HashMap, ArrayBuffer,ListBuffer,Map=>MMap}
import scala.util.matching.Regex
import trellis.data._
import trellis.data.FileExtensionRegexes._
import trellis.process.catalog._
import trellis.RasterExtent
import trellis.operation._
import trellis.raster.IntRaster
import scala.math.min

import sys.error

/**
 * Messages sent by Server-related classes
 */

/**
 * Message: Send to server to create new task group.
 */ 
case class CreateTaskGroup(args:List[Any], callback:(Option[Results]) => Any)

/**
 * Message: Send to TaskGroup to start tasks.
 */
case class StartTasks()

/**
 * Message: Send to server or dispatcher to run an operation. 
 */
case class RunOperation[T](op:Operation[T], gid:Int, position:Int) 

/**
 * Message: Send to worker to run an operation with the associated server.
 */ 
case class WorkerTask[T](task:RunOperation[T], server:Server)

/**
 * Message: The result of an individual operation.
 */
case class OperationResult[T](result:T, tgid:Int, position:Int)

/**
 * TaskGroup is complete
 */
case class TaskGroupComplete(tgid:Int)
 

/**
 * Results from a task group; can include literal values that are passed through.
 * Used to start steps of an Operation, and to return the final result.
 */
case class Results(results:List[Any]) 

object Dummy {
  //type Callback = (Any) => Unit
  //type PF = PartialFunction[Any, Any]
  System.setProperty( "actors.enableForkJoin", "false" )
}

import Dummy._

/**
  * Abstract class which is responsible for dispatching and executing operations.
  */
class Server(id:String, val catalog:Catalog) extends Actor with FileCaching {

  // next TaskGroup ID
  var nextId = 0
  var debug = false

  // Map of task groups ids -> task groups
  val taskGroups = MMap[Int,TaskGroup]()

  val dispatcher:Dispatcher = new Dispatcher(this)
  dispatcher.start

  def server:Server = this

  def run[T] (op:Operation[T]):T = {

    //val result = (this !? op).asInstanceOf[T]
    val result = (this !? op).asInstanceOf[Option[T]]
    result match {
      case Some(r) => r
      case None => error("server(%s) returned no result".format(op))
    }
  }

  def act() = {
    loop {
      react {
        case CreateTaskGroup(args:List[_], tgCallback: Any ) => {
          val tgid = nextId
          val tg = new TaskGroup(tgid, args, tgCallback)
          nextId = nextId + 1
          taskGroups(tgid) = tg
          tg.start
          tg ! StartTasks() 
        } 
        case runMsg:RunOperation[_] => {
          dispatcher ! runMsg
        }
        case op:Operation[_] => {
          val x = makeCallback(sender)
          this ! CreateTaskGroup(List(op), x) 
        }
        case r:OperationResult[_] => {
          taskGroups(r.tgid) ! r 
        }
        // TaskGroup has signalled that it is finished.
        case TaskGroupComplete(tgid:Int) => {
          taskGroups.remove(tgid)
        }
        
        case a:Any => { 
          println("Received unknown message: %s".format( a.toString() ) ) 
          exit()
        } 
      }
    }
  }

  override def exceptionHandler = {
    case e: Exception => {
      println("** Exception in server: %s".format(e.getMessage()))
      exit()
    }
  }

  def makeCallback(sender:OutputChannel[Any]) = {
    sendAnswerToSender(sender.toString(), sender, _:Any)
  }

  def sendAnswerToSender(id:String, client:OutputChannel[Any], result:Any) {
    result match {
      case Some(Results(List(a:Any))) => client ! Some(a)
      case _ => client ! None
    }
  }
}



class Worker(server:Server) extends Actor {
  var debug = false
  def act = {
    react {
      case WorkerTask(RunOperation(op, gid, position), server) => {
        if (debug) printf("  Worker: Received operation (%d, %d) to execute: %s\n\n", gid, position, op )
        try {
          op.run(server, sendResult(gid, position, sender, _:Any) )
        } catch {
          case e:Exception => {
            e.printStackTrace()
            sendResult(gid, position, sender, None) 
          }
        }
        exit()
      }
      case x:Any => throw new Exception("Worker received unknown operation: %s".format(x.toString()))
    } 
   }
  override def exceptionHandler = {
    case e: Exception => {
      println("worker exception")
      e.printStackTrace()
      server ! e
    }
  }
  def sendResult( tgid:Int, position:Int, client:OutputChannel[Any], result:Any) = {
    if (debug) printf("  Worker (%s): Returning result of operation (%d, %d): %s\n\n", this, tgid, position, result)
    client ! OperationResult(result, tgid, position)
  }

}


/**
 * * Default dispatcher simply runs the operation and returns the result.
 * As it is a scala actor using reply, each execution is being scheduled on
 * a threadpool.  Override Dispatcher, however, to support other schedulers,
 * remote execution, etc.
 */
class Dispatcher(server:Server) extends Actor {
  var debug = false
  def sendResult( tgid:Int, position:Int, client:OutputChannel[Any], result:Any) = {
    client ! OperationResult(result, tgid, position)
    this.exit()
  }

  def act() = {
    loop {
      react {
        case RunOperation(op:Operation[_], gid:Int, position:Int) => {
          val worker = new Worker(server)
          worker.debug = debug
          worker.start
          
          worker ! WorkerTask(RunOperation(op, gid, position),server)
          //op.run(server, sendResult(gid, position, sender, _:Any))
        }
        case r:OperationResult[_] => {
          server ! r
        }
        case r:Any => {
          println("Dispatcher received unknown result.")
        }
      }
    }
  }
}

class TaskGroup (id:Int, args:List[Any], callback:(Option[Results]) => Any) extends Actor {
  var working = true

  type OpTuple = (Int,Operation[_])

  var (results:Array[Any], ops:List[_]) = initialize(args)
  
  def initialize(args:List[Any]) = {
    val _results = Array.fill[Any](args.length)('waiting)
    val _ops = ListBuffer[OpTuple]()

    for (i <- 0 until args.length) {
      args(i) match {
        case op:Operation[_] => { _ops.append( (i,op) ) }
        case other:Any => { _results(i) = other }        
        case null => { _results(i) = null }
      }
    }
    (_results, _ops.toList)
  }

  def startTasks(server:Server) = {
    ops.foreach {
      case (i:Int, op:Operation[_]) => {
        server ! RunOperation(op, id, i) 
      }
    }
  }

  def act() = {
    loopWhile(working) {
      react {
        case g:StartTasks => {
          if (ops.length == 0) {
            callback( Some(Results ( results.toList )) )
          } else {
            ops.foreach {
              case (i:Int, op:Operation[_]) => {
                sender ! RunOperation(op, id, i)
              }
            }
          }
        }

        case OperationResult(None, _, _) => callback(None)

        case OperationResult(result:Option[_], tgid:Int, position:Int) => {
          results(position) = result.get
          val done =  // false if any results == None
            results.foldLeft (true) {(flag,v) => if (v == 'waiting) false else flag}
          if (done) {
            callback( Some(Results( results.toList)))
            results = null
            ops = null
            working = false // will end actor
            sender ! TaskGroupComplete(this.id)
          }

        }
        case g:Any => { println(" * * TaskGroup received unknown message." + g.toString()) }
      }
    } 
  }
  override def exceptionHandler = {
    case e: Exception => {
       println("** Exception in TaskGroup: %s".format(e.getMessage()))
      e.printStackTrace()
      callback(None)
    }
  } 
}

object TaskGroup {
  def apply(id:Int, args:List[Any], callback:(Option[Results]) => Any) = new TaskGroup(id, args, callback)
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

  def enableCaching { caching = true }
  def disableCaching { caching = false }

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

  def server:Server
}

object Server {
  val catalogPath = "/etc/trellis/catalog.json"

  def apply(id:String): Server = Server(id, Catalog.fromPath(catalogPath))

  def apply(id:String, catalog:Catalog): Server = {
    val server = new Server(id, catalog)
    server.start
    server
  }
}

object TestServer {
  def apply() = Server("test", Catalog.empty("test"))
}
