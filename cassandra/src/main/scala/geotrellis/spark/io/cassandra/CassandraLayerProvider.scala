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

package geotrellis.spark.io.cassandra

import geotrellis.spark.io.cassandra.conf.CassandraConfig
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util.UriUtils
import org.apache.spark.SparkContext
import java.net.URI

import geotrellis.layers.LayerId
import geotrellis.layers.io.{ValueReader, ValueReaderProvider}

/**
 * Provides [[CassandraAttributeStore]] instance for URI with `cassandra` scheme.
 *  ex: `cassandra://[user:password@]zookeeper[:port][/keyspace][?attributes=table1[&layers=table2]]`
 *
 * Metadata table name is optional, not provided default value will be used.
 * Layers table name is required to instantiate a [[LayerWriter]]
 */
class CassandraLayerProvider extends AttributeStoreProvider
    with LayerReaderProvider with LayerWriterProvider with ValueReaderProvider with CollectionLayerReaderProvider {

  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "cassandra") true else false
    case null => false
  }

  def attributeStore(uri: URI): AttributeStore = {
    val params = UriUtils.getParams(uri)
    val instance = CassandraInstance(uri)
    val attributeTable = params.getOrElse("attributes", CassandraConfig.catalog)
    val keyspace = Option(uri.getPath.drop(1)).getOrElse(CassandraConfig.keyspace)
    CassandraAttributeStore(instance, keyspace, attributeTable)
  }

  def layerReader(uri: URI, store: AttributeStore, sc: SparkContext): FilteringLayerReader[LayerId] = {
    val instance = CassandraInstance(uri)
    new CassandraLayerReader(store, instance)(sc)
  }

  def layerWriter(uri: URI, store: AttributeStore): LayerWriter[LayerId] = {
    val instance = CassandraInstance(uri)
    val keyspace = Option(uri.getPath.drop(1)).getOrElse(CassandraConfig.keyspace)
    val params = UriUtils.getParams(uri)
    val table = params.getOrElse("layers",
      throw new IllegalArgumentException("Missing required URI parameter: layers"))

    new CassandraLayerWriter(store, instance, keyspace, table)
  }

  def valueReader(uri: URI, store: AttributeStore): ValueReader[LayerId] = {
    val instance = CassandraInstance(uri)
    new CassandraValueReader(instance, store)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore): CollectionLayerReader[LayerId] = {
    val instance = CassandraInstance(uri)
    new CassandraCollectionLayerReader(store, instance)
  }
}
