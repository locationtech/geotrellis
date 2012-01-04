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

  def toPretty() = _toPretty(0)
  private def _toPretty(indent:Int):String = {
    val pad = "  " * indent
    var s = "%s%s %d ms\n" format (pad, id, stopTime - startTime)
    children.foreach {
      s += _._toPretty(indent + 1)
    }
    s
  }

  def toDetailed(indent:Int = 0):String = {
    val pad = " " * indent 
    var s = pad + " -- %s\n" format id 
    val elapsed = stopTime - startTime
    s += pad + " -- elapsed: %d\n" format elapsed 
    s += pad + " -- times: %d -> %d\n" format (startTime % 1000, stopTime % 1000)
    children.foreach { s += _.toDetailed(indent + 2) } 
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

trait TimerLike {
  def add(h:History):Unit
  def children:List[History]
  def toSuccess(name:String, start:Long, stop:Long) = {
    Success(name, start, stop, children)
  }
  def toFailure(name:String, start:Long, stop:Long, msg:String, trace:String) = {
    Failure(name, start, stop, children, msg, trace)
  }
}

object TimerLike {
  implicit val need = NeedTimer
}

object NeedTimer extends TimerLike {
  def add(h:History) {}
  def children = Nil
}

class Timer extends TimerLike {
  private val histories = new ArrayBuffer[History]
  def add(h:History) { histories.append(h) }
  def children = histories.toList
}
