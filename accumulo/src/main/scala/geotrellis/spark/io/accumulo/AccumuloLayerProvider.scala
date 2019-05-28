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
import geotrellis.spark.io.accumulo.conf.AccumuloConfig
import geotrellis.util.UriUtils
import org.apache.spark.SparkContext
import java.net.URI

import geotrellis.layers.LayerId
import geotrellis.layers.io.{ValueReader, ValueReaderProvider}

/**
 * Provides [[AccumuloAttributeStore]] instance for URI with `accumulo` scheme.
 *  ex: `accumulo://[user[:password]@]zookeeper/instance-name[?attributes=table1[&layers=table2]]`
 *
 * Attributes table name is optional, not provided default value will be used.
 * Layers table name is required to instantiate a [[LayerWriter]]
 */
class AccumuloLayerProvider extends AttributeStoreProvider
    with LayerReaderProvider with LayerWriterProvider with ValueReaderProvider with CollectionLayerReaderProvider {

  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "accumulo") true else false
    case null => false
  }

  def attributeStore(uri: URI): AttributeStore = {
    val instance = AccumuloInstance(uri)
    val params = UriUtils.getParams(uri)
    val attributeTable = params.getOrElse("attributes", AccumuloConfig.catalog)
    AccumuloAttributeStore(instance, attributeTable)
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

  def valueReader(uri: URI, store: AttributeStore): ValueReader[LayerId] = {
    val instance = AccumuloInstance(uri)
    new AccumuloValueReader(instance, store)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore): CollectionLayerReader[LayerId] = {
    val instance = AccumuloInstance(uri)
    new AccumuloCollectionLayerReader(store)(instance)
  }

}
