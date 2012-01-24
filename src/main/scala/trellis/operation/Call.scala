package trellis.operation

import trellis.process._

/**
  * Multiply each cell in a raster by a constant value.
  */
case class Call[A:Manifest, B:Manifest](v:Op[A])(f:A => B) extends Operation[B] {
  def _run(context:Context) = runAsync(List(v))
  val nextSteps:Steps = {
    case v => Result(f(v.asInstanceOf[A]))
  }
}
