package geotrellis.spark.io

import geotrellis.spark.LayerId

trait AttributeCaching { self: AttributeStore =>
  private val cache = new collection.mutable.HashMap[(LayerId, String), Any]

  def cacheRead[T: ReadableWritable](layerId: LayerId, attributeName: String): T = {
    cache.getOrElseUpdate(layerId -> attributeName, read[T](layerId, attributeName)).asInstanceOf[T]
  }

  def cacheWrite[T: ReadableWritable](layerId: LayerId, attributeName: String, value: T): Unit = {
    cache.update(layerId -> attributeName, value)
    write[T](layerId, attributeName, value)
  }

  def clearCache() = {
    cache.clear()
  }
}