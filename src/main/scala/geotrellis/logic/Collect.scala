package geotrellis.logic

import geotrellis._
import geotrellis._
import geotrellis.process._

/**
 * Takes a sequence of operations, and returns a Sequence of the results of those operations.
 */
case class Collect[A](ops:Op[Seq[Op[A]]]) extends Op[Seq[A]] {
  def _run(context:Context) = runAsync(List('init, ops)) 
  val nextSteps:Steps = {
    case 'init :: (opSeq:Seq[_]) :: Nil => runAsync('eval :: opSeq.toList)
    case 'eval :: (as:List[_]) => Result(as.asInstanceOf[List[A]].toSeq) 
    case _ => throw new Exception("unexpected stepresult")
  }
}


object CollectArray {
  @deprecated("Use Collect.","0.9")
  def apply[A:Manifest](ops:Array[Op[A]]):Op[Array[A]] = Collect(Literal(ops.toSeq)).map( _.toArray ) 
}
