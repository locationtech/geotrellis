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
