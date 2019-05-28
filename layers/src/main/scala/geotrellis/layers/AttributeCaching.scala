package geotrellis.layers

import geotrellis.layers.hadoop.conf.AttributeConfig

import com.github.blemale.scaffeine.Scaffeine

import scala.concurrent.duration._

import spray.json._
import spray.json.DefaultJsonProtocol._


trait AttributeCaching { self: AttributeStore =>
  @transient private lazy val cache =
    Scaffeine()
      .recordStats()
      .expireAfterWrite(AttributeConfig.caching.expirationMinutes.minutes)
      .maximumSize(AttributeConfig.caching.maxSize)
      .build[(LayerId, String), JsValue]

  def cacheRead[T: JsonFormat](layerId: LayerId, attributeName: String): T =
    if(AttributeConfig.caching.enabled)
      cache.get(layerId -> attributeName, { _ => read[JsValue](layerId, attributeName) }).convertTo[T]
    else
      read[JsValue](layerId, attributeName).convertTo[T]

  def cacheLayerType(layerId: LayerId, layerType: LayerType): LayerType =
    if (AttributeConfig.caching.enabled)
      cache.get(layerId -> "layerType", { _ => layerType.toJson }).convertTo[LayerType]
    else
      layerType

  def cacheWrite[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    if(AttributeConfig.caching.enabled) cache.put(layerId -> attributeName, value.toJson)
    write[T](layerId, attributeName, value)
  }

  def clearCache(): Unit = {
    if(AttributeConfig.caching.enabled) cache.invalidateAll()
  }

  def clearCache(id: LayerId): Unit = {
    if(AttributeConfig.caching.enabled) {
      val toInvalidate = cache.asMap.keys.filter(_._1 == id)
      cache.invalidateAll(toInvalidate)
    }
  }

  def clearCache(id: LayerId, attribute: String): Unit = {
    if(AttributeConfig.caching.enabled) cache.invalidate(id -> attribute)
  }
}
