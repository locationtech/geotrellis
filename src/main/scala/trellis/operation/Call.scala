package trellis.operation

import trellis.process._

/**
  * Multiply each cell in a raster by a constant value.
  */
case class Call[A:Manifest, B:Manifest](v:Op[A])(f:A => B) extends SimpleOp[B] {
  def _value(context:Context) = f(context.run(v))
}
