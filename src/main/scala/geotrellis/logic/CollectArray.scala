package geotrellis.logic

import geotrellis._
import geotrellis.process._
import geotrellis._

/**
 * Takes a sequence of operations, and returns an Array of the results of those operations.
 */
case class CollectArray[A:Manifest](ops:Array[Op[A]]) extends Op[Array[A]] {
  def _run(context:Context) = runAsync(ops.toList)
  val nextSteps:Steps = {
    case as:List[_] => Result(as.asInstanceOf[List[A]].toArray)
  }
}
