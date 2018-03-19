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

import geotrellis.spark.LayerId

import com.github.blemale.scaffeine.Scaffeine
import spray.json.JsonFormat
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

trait AttributeCaching { self: AttributeStore =>
  @transient private lazy val (enabled, cache) = {
    val config = ConfigFactory.load()
    val expiration = config.getInt("geotrellis.attribute.caching.expirationMinutes")
    val maxSize = config.getInt("geotrellis.attribute.caching.maxSize")
    val enabled = config.getBoolean("geotrellis.attribute.caching.enabled")

    enabled -> Scaffeine()
      .recordStats()
      .expireAfterWrite(expiration.minutes)
      .maximumSize(maxSize)
      .build[(LayerId, String), Any]
  }

  def cacheRead[T: JsonFormat](layerId: LayerId, attributeName: String): T = {
    if(enabled)
      cache.get(layerId -> attributeName, { _ => read[T](layerId, attributeName) }).asInstanceOf[T]
    else
      read[T](layerId, attributeName)
  }

  def cacheWrite[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    if(enabled) cache.put(layerId -> attributeName, value)
    write[T](layerId, attributeName, value)
  }

  def clearCache(): Unit = {
    if(enabled) cache.invalidateAll()
  }

  def clearCache(id: LayerId): Unit = {
    if(enabled) {
      val toInvalidate = cache.asMap.keys.filter(_._1 == id)
      cache.invalidateAll(toInvalidate)
    }
  }

  def clearCache(id: LayerId, attribute: String): Unit = {
    if(enabled) cache.invalidate(id -> attribute)
  }
}
