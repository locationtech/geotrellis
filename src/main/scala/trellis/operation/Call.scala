package trellis.operation

import trellis.process.Server

/**
  * Multiply each cell in a raster by a constant value.
  */
case class Call[A, B](r:Operation[A], f:A => B) extends SimpleOperation[B] {
  def childOperations = List(r)
  def _value(server:Server) = f(server.run(r))
}
