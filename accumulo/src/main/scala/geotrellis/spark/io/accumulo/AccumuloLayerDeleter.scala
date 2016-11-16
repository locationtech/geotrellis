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

package geotrellis.spark.io.accumulo

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import org.apache.accumulo.core.client.{BatchWriterConfig, Connector}
import org.apache.accumulo.core.security.Authorizations
import spray.json.JsonFormat
import org.apache.accumulo.core.data.{Range => AccumuloRange}
import spray.json.DefaultJsonProtocol._
import scala.collection.JavaConversions._

class AccumuloLayerDeleter(val attributeStore: AttributeStore, connector: Connector) extends LayerDeleter[LayerId] {

  def delete(id: LayerId): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val header = try {
      attributeStore.readHeader[AccumuloLayerHeader](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(id).initCause(e)
    }

    val numThreads = 1
    val config = new BatchWriterConfig()
    config.setMaxWriteThreads(numThreads)
    val deleter = connector.createBatchDeleter(header.tileTable, new Authorizations(), numThreads, config)
    deleter.fetchColumnFamily(columnFamily(id))
    deleter.setRanges(new AccumuloRange() :: Nil)
    deleter.delete()

    attributeStore.delete(id)
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
