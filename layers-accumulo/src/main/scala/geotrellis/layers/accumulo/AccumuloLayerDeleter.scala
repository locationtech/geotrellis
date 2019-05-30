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

package geotrellis.layers.accumulo

import geotrellis.layers._

import com.typesafe.scalalogging.LazyLogging

import org.apache.accumulo.core.client.{BatchWriterConfig, Connector}
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data.{Range => AccumuloRange}

import scala.collection.JavaConverters._

class AccumuloLayerDeleter(val attributeStore: AttributeStore, connector: Connector) extends LazyLogging with LayerDeleter[LayerId] {

  def delete(id: LayerId): Unit = {
    try {
      val header = attributeStore.readHeader[AccumuloLayerHeader](id)
      val numThreads = 1
      val config = new BatchWriterConfig()
      config.setMaxWriteThreads(numThreads)
      val deleter = connector.createBatchDeleter(header.tileTable, new Authorizations(), numThreads, config)
      try {
        deleter.fetchColumnFamily(columnFamily(id))
        deleter.setRanges(List(new AccumuloRange()).asJava)
        deleter.delete()
      } finally {
        deleter.close()
      }
    } catch {
      case e: AttributeNotFoundError =>
        logger.info(s"Metadata for $id was not found. Any associated layer data (if any) will require manual deletion")
        throw new LayerDeleteError(id).initCause(e)
    } finally {
      attributeStore.delete(id)
    }
  }
}

object AccumuloLayerDeleter {
  def apply(attributeStore: AttributeStore, connector: Connector): AccumuloLayerDeleter =
    new AccumuloLayerDeleter(attributeStore, connector)

  def apply(attributeStore: AttributeStore, instance: AccumuloInstance): AccumuloLayerDeleter =
    new AccumuloLayerDeleter(attributeStore, instance.connector)

  def apply(attributeStore: AccumuloAttributeStore): AccumuloLayerDeleter =
    new AccumuloLayerDeleter(attributeStore, attributeStore.connector)

  def apply(instance: AccumuloInstance): AccumuloLayerDeleter =
    apply(AccumuloAttributeStore(instance.connector), instance.connector)
}
