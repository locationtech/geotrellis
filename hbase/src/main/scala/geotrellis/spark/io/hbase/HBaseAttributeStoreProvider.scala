/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.io.hbase

import geotrellis.spark._
import geotrellis.spark.io._

import java.net.URI


/**
 * Provides [[HBaseAttributeStore]] instance for URI with `cassandra` scheme.
 *  ex: `hbase://zookeeper[:port][?master=host]#metadata-table-name]`
 *
 * Metadata table name is optional, not provided default value will be used.
 */
class HBaseAttributeStoreProvider extends AttributeStoreProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme.toLowerCase == "hbase"

  def attributeStore(uri: URI): AttributeStore = {
    val zookeeper = uri.getHost
    val port = Option(uri.getPort).getOrElse(2181)
    val attributeTable = uri.getFragment
    val params = getParams(uri)
    val instance = HBaseInstance(
      List(zookeeper),
      params.getOrElse("master", ""),
      port.toString)

    if (null == attributeTable)
      HBaseAttributeStore(instance)
    else
      HBaseAttributeStore(instance, attributeTable)
  }
}
