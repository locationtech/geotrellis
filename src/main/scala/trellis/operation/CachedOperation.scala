package trellis.operation

import trellis.process._

/**
 * Trait providing caching support for operations which want to save their
 * result and return those on future invocations of run().
 */
trait CachedOperation[T] extends Op[T] {
  var cachedOutput:Option[StepOutput[T]] = None

  def cacheOutput(output:StepOutput[T]) = {
    cachedOutput = Some(output)
    output
  }

  override def run(server:Server): StepOutput[T] = {
    cachedOutput match {
      case Some(o) => { println("Cached operation.  woot."); o }
      case None => {
        startTime = System.currentTimeMillis()
        cacheOutput(this._run(server))
      }
    }
  }
}

case class Cache[T:Manifest](op:Op[T])
extends SimpleOperation[T] with CachedOperation[T] {
  override def childOperations = List(op)
  def _value(server:Server) = server.run(op)
}
