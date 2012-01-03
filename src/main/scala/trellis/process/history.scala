package trellis.process

import scala.collection.mutable.ArrayBuffer

import trellis.operation._

/**
 * History stores information about the execution of an operation.
 */
sealed trait History {
  def id:String
  def info:Map[String, Any] = Map.empty[String, Any]

  def startTime:Long
  def stopTime:Long
  def children:List[History]

  def isOriginalFailure = originalFailures == List(this)

  def originalFailures:List[Failure] = this match {
    case t:Success => Nil
    case t:Failure => children.flatMap(_.originalFailures) match {
      case Nil => List(t)
      case failures => failures
    }
  }

  def toPretty(indent:Int = 0):String = {
    val pad = " " * indent 
    var s = pad + " -- %s\n" format id 
    val elapsed = stopTime - startTime
    s += pad + " -- elapsed: %d\n" format elapsed 
    s += pad + " -- times: %d -> %d\n" format (startTime % 1000, stopTime % 1000)
    children.foreach { s += _.toPretty(indent + 2) } 
    s
  }
}

/**
 * Success is the History of a successful operation.
 */
case class Success(id:String, startTime:Long, stopTime:Long,
                   children:List[History]) extends History

/**
 * Failure is the History of a failed operation.
 */
case class Failure(id:String, startTime:Long, stopTime:Long,
                   children:List[History], message:String,
                   trace:String) extends History {}

trait TimerLike

object TimerLike {
  implicit val need = NeedTimer
}

object NeedTimer extends TimerLike

class Timer(name:String) extends TimerLike {
  private var startTime = 0L
  private var stopTime = 0L

  val timers = new ArrayBuffer[Timer]

  def add(t:Timer) { timers.append(t) }

  def start() { startTime = time() }
  def stop() { stopTime = time() }

  def toHistory: History = Success("timer " + name, startTime, stopTime,
                                   timers.toList.map(_.toHistory))

  def createChild(name:String) = {
    val t2 = new Timer(name)
    this.add(t2)
    t2
  }
}

object Timer {
  def fromOp(op:Operation[_]) = new Timer(op.getClass.getName)
}
