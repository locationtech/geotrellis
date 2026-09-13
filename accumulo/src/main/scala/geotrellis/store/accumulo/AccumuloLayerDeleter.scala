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

package geotrellis.store.accumulo

import geotrellis.store._

import org.log4s._

import org.apache.accumulo.core.client.{AccumuloClient, BatchWriterConfig}
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data.{Range => AccumuloRange}

import scala.jdk.CollectionConverters._

class AccumuloLayerDeleter(val attributeStore: AttributeStore, client: AccumuloClient) extends LayerDeleter[LayerId] {
  @transient private[this] lazy val logger = getLogger

  def delete(id: LayerId): Unit = {
    try {
      val header = attributeStore.readHeader[AccumuloLayerHeader](id)
      val numThreads = 1
      val config = new BatchWriterConfig()
      config.setMaxWriteThreads(numThreads)
      val deleter = client.createBatchDeleter(header.tileTable, new Authorizations(), numThreads, config)
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
  def apply(attributeStore: AttributeStore, client: AccumuloClient): AccumuloLayerDeleter =
    new AccumuloLayerDeleter(attributeStore, client)

  def apply(attributeStore: AttributeStore, instance: AccumuloInstance): AccumuloLayerDeleter =
    new AccumuloLayerDeleter(attributeStore, instance.client)

  def apply(attributeStore: AccumuloAttributeStore): AccumuloLayerDeleter =
    new AccumuloLayerDeleter(attributeStore, attributeStore.client)

  def apply(instance: AccumuloInstance): AccumuloLayerDeleter =
    apply(AccumuloAttributeStore(instance.client), instance.client)
}
