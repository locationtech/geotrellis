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

package geotrellis.store.hbase

import geotrellis.layers._
import geotrellis.store.hbase.conf.HBaseConfig
import geotrellis.store.hbase._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util.UriUtils
import org.apache.spark.SparkContext
import java.net.URI


/**
 * Provides [[HBaseAttributeStore]] instance for URI with `hbase` scheme.
 *  ex: `hbase://zookeeper[:port][?master=host][?attributes=table1[&layers=table2]]`
 *
 * Metadata table name is optional, not provided default value will be used.
 * Layers table name is required to instantiate a [[LayerWriter]]
 */
class HBaseCollectionLayerProvider
  extends AttributeStoreProvider
     with ValueReaderProvider
     with CollectionLayerReaderProvider {

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

  def valueReader(uri: URI, store: AttributeStore): ValueReader[LayerId] = {
    val instance = HBaseInstance(uri)
    new HBaseValueReader(instance, store)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore): CollectionLayerReader[LayerId] = {
    val instance = HBaseInstance(uri)
    new HBaseCollectionLayerReader(store, instance)
  }
}
