package geotrellis.process

import scala.collection.mutable.ArrayBuffer

/**
 * Timers are used to accumulate child results, and create history objects.
 */
class Timer {
  private val histories = new ArrayBuffer[History]

  def add(h:History) { histories.append(h) }
  def children = histories.toList

  def toSuccess(name:String, start:Long, stop:Long) = {
    Success(name, start, stop, children)
  }
  def toFailure(name:String, start:Long, stop:Long, msg:String, trace:String) = {
    Failure(name, start, stop, children, msg, trace)
  }
}