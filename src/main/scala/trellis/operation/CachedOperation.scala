package trellis.operation

import trellis.process.Server

/**
 * Trait providing caching support for operations which want to save their
 * result and return those on future invocations of run().
 */
trait CachedOperation[T] extends Operation[T] {
  var cachedResult:Option[T] = None

  def cacheResult(result:Any, callback:Callback ) = {
    cachedResult = result.asInstanceOf[Option[T]]
    callback(cachedResult)
  }

  override def run(server:Server, callback:Callback): Unit = {
    if (cachedResult.isEmpty) {
      startTime = System.currentTimeMillis()
      this._run(server, cacheResult(_:Any, callback))
    } else {
      println("Cached operation.  woot.")
      callback(cachedResult)
    }
  }
}

case class Cache[T:Manifest](op: Operation[T]) extends SimpleOperation[T] with CachedOperation[T] {
  override def childOperations = List(op)
  def _value(server:Server) = server.run(op)
}
