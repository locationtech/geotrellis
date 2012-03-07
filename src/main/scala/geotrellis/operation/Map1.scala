package geotrellis.operation

import geotrellis.process._

/**
 * Map an Op[A] into an Op[Z] using a function from A => Z.
 */
case class Map1[A:Manifest, Z:Manifest](a:Op[A])(call:A => Z) extends Op[Z] {
  def _run(context:Context) = runAsync(a :: Nil)
  val nextSteps:Steps = {
    case a :: Nil => Result(call(a.asInstanceOf[A]))
  }
}
