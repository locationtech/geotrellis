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

package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util.UriUtils
import org.apache.spark.SparkContext
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import java.net.URI


/**
 * Provides [[AccumuloAttributeStore]] instance for URI with `accumulo` scheme.
 *  ex: `accumulo://[user[:password]@]zookeeper/instance-name[?attributes=table1[&layers=table2]]`
 *
 * Attributes table name is optional, not provided default value will be used.
 * Layers table name is required to instantiate a [[LayerWriter]]
 */
class AccumuloLayerProvider extends AttributeStoreProvider with LayerReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme.toLowerCase == "accumulo"

  def attributeStore(uri: URI): AttributeStore = {
    val instance = AccumuloInstance(uri)
    val params = UriUtils.getParams(uri)
    params.get("attributes") match {
      case Some(attributeTable) =>
        AccumuloAttributeStore(instance, attributeTable)
      case None =>
        AccumuloAttributeStore(instance)
    }
  }

  def layerReader(uri: URI, store: AttributeStore, sc: SparkContext): FilteringLayerReader[LayerId] = {
    val instance = AccumuloInstance(uri)

    new AccumuloLayerReader(store)(sc, instance)
  }

  def layerWriter(uri: URI, store: AttributeStore): LayerWriter[LayerId] = {
    val instance = AccumuloInstance(uri)
    val params = UriUtils.getParams(uri)
    val table = params.getOrElse("layers",
      throw new IllegalArgumentException("Missing required URI parameter: layers"))

    AccumuloLayerWriter(instance, store, table)
  }
}
