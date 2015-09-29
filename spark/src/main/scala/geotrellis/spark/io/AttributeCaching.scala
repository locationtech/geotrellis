package geotrellis.spark.io

import geotrellis.spark.LayerId

trait AttributeCaching[F[_]] { self: AttributeStore[F] =>
  private val cache = new collection.mutable.HashMap[(LayerId, String), Any]

  def cacheRead[T: Format](layerId: LayerId, attributeName: String): T = {
    cache.getOrElseUpdate(layerId -> attributeName, read[T](layerId, attributeName)).asInstanceOf[T]
  }

  def cacheWrite[T: Format](layerId: LayerId, attributeName: String, value: T): Unit = {
    cache.update(layerId -> attributeName, value)
    write[T](layerId, attributeName, value)
  }

  def clearCache() = {
    cache.clear()
  }
}