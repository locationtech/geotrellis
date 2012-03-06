package geotrellis.operation

import geotrellis.process._

/**
 * Map an Op[A] and Op[B] into an Op[Z] using a function from (A,B) => Z.
 */
case class Map2[A:Manifest, B:Manifest, Z:Manifest]
(a:Op[A], b:Op[B])(call:(A,B) => Z) extends Op[Z] {
  def _run(context:Context) = runAsync(a :: b :: Nil)
  val nextSteps:Steps = {
    case a :: b :: Nil => Result(call(a.asInstanceOf[A],
                                      b.asInstanceOf[B]))
  }
}
