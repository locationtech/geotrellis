package geotrellis.spark.io

import geotrellis.spark.LayerId

import spray.json.JsonFormat

trait AttributeCaching { self: AttributeStore =>
  private val cache = new collection.mutable.HashMap[(LayerId, String), Any]

  def cacheRead[T: JsonFormat](layerId: LayerId, attributeName: String): T = {
    cache.getOrElseUpdate(layerId -> attributeName, read[T](layerId, attributeName)).asInstanceOf[T]
  }

  def cacheWrite[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    cache.update(layerId -> attributeName, value)
    write[T](layerId, attributeName, value)
  }

  def clearCache(): Unit = {
    cache.clear()
  }

  def clearCache(id: LayerId): Unit = {
    cache.keySet.filter(_._1 == id).foreach(cache.remove)
  }

  def clearCache(id: LayerId, attribute: String): Unit = {
    cache.remove((id, attribute))
  }
}
