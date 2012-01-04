package trellis.operation

import trellis.process._

/**
  * Multiply each cell in a raster by a constant value.
  */
case class Call[A:Manifest, B](r:Operation[A], f:A => B) extends SimpleOperation[B] {
  def childOperations = List(r)
  def _value(context:Context) = f(context.run(r))
}
