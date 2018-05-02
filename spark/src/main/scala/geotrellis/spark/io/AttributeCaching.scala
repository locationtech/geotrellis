/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io

import geotrellis.spark._
import com.github.blemale.scaffeine.Scaffeine
import geotrellis.spark.io.hadoop.conf.AttributeConfig

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
