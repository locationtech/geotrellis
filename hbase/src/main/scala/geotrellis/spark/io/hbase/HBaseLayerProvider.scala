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
import geotrellis.spark.io.hbase.conf.HBaseConfig
import geotrellis.util.UriUtils
import org.apache.spark.SparkContext
import java.net.URI

import geotrellis.layers.LayerId
import geotrellis.layers.io.{ValueReader, ValueReaderProvider}

/**
 * Provides [[HBaseAttributeStore]] instance for URI with `hbase` scheme.
 *  ex: `hbase://zookeeper[:port][?master=host][?attributes=table1[&layers=table2]]`
 *
 * Metadata table name is optional, not provided default value will be used.
 * Layers table name is required to instantiate a [[LayerWriter]]
 */
class HBaseLayerProvider extends AttributeStoreProvider
    with LayerReaderProvider with LayerWriterProvider with ValueReaderProvider with CollectionLayerReaderProvider {

  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "hbase") true else false
    case null => false
  }

  def attributeStore(uri: URI): AttributeStore = {
    val instance = HBaseInstance(uri)
    val params = UriUtils.getParams(uri)
    val attributeTable = params.getOrElse("attributes", HBaseConfig.catalog)
    HBaseAttributeStore(instance, attributeTable)
  }

  def layerReader(uri: URI, store: AttributeStore, sc: SparkContext): FilteringLayerReader[LayerId] = {
    val instance = HBaseInstance(uri)
    new HBaseLayerReader(store, instance)(sc)
  }

  def layerWriter(uri: URI, store: AttributeStore): LayerWriter[LayerId] = {
    val instance = HBaseInstance(uri)
    val params = UriUtils.getParams(uri)
    val table = params.getOrElse("layers",
      throw new IllegalArgumentException("Missing required URI parameter: layers"))
    new HBaseLayerWriter(store, instance, table)
  }

  def valueReader(uri: URI, store: AttributeStore): ValueReader[LayerId] = {
    val instance = HBaseInstance(uri)
    new HBaseValueReader(instance, store)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore): CollectionLayerReader[LayerId] = {
    val instance = HBaseInstance(uri)
    new HBaseCollectionLayerReader(store, instance)
  }

}
