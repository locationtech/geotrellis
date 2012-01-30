package trellis.operation

import trellis.process._

/**
  * Multiply each cell in a raster by a constant value.
  */
case class Call[A:Manifest, Z:Manifest](a:Op[A])(call:A => Z) extends Op[Z] {
  def _run(context:Context) = runAsync(a :: Nil)
  val nextSteps:Steps = {
    case a :: Nil => Result(call(a.asInstanceOf[A]))
  }
}

case class Call2[A:Manifest, B:Manifest, Z:Manifest]
(a:Op[A], b:Op[B])(call:(A,B) => Z) extends Op[Z] {
  def _run(context:Context) = runAsync(a :: b :: Nil)
  val nextSteps:Steps = {
    case a :: b :: Nil => Result(call(a.asInstanceOf[A],
                                      b.asInstanceOf[B]))
  }
}
