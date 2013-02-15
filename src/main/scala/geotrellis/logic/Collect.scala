package geotrellis.logic

import geotrellis._
import geotrellis._
import geotrellis.process._

/**
 * Takes a sequence of operations, and returns a Sequence of the results of those operations.
 */
case class Collect[A](ops:Seq[Op[A]]) extends Op[Seq[A]] {
  def _run(context:Context) = runAsync(ops.toList)
  val nextSteps:Steps = {
    case as:List[_] => Result(as.asInstanceOf[List[A]].toSeq) 
  }
}
