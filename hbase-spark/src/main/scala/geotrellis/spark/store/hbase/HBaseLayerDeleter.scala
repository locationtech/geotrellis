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

package geotrellis.spark.store.hbase

import geotrellis.layers._
import geotrellis.store.hbase._
import geotrellis.spark.store._
import com.typesafe.scalalogging.LazyLogging
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter.PrefixFilter

import scala.collection.JavaConverters._

class HBaseLayerDeleter(val attributeStore: AttributeStore, instance: HBaseInstance) extends LazyLogging with LayerDeleter[LayerId] {

  def delete(id: LayerId): Unit = {
    try{
      val header = attributeStore.readHeader[HBaseLayerHeader](id)

      // Deletion list should be mutable
      val list = new java.util.ArrayList[Delete]()
      val scan = new Scan()
      scan.addFamily(hbaseTileColumnFamily)
      scan.setFilter(new PrefixFilter(hbaseLayerIdString(id)))

      instance.withTableConnectionDo(header.tileTable) { table =>
        val scanner = table.getScanner(scan)
        try {
          scanner.iterator().asScala.foreach { kv =>
            val delete = new Delete(kv.getRow)
            delete.addFamily(hbaseTileColumnFamily)
            list.add(delete)
          }
        } finally scanner.close()

        table.delete(list)
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

object HBaseLayerDeleter {
  def apply(attributeStore: AttributeStore, instance: HBaseInstance): HBaseLayerDeleter =
    new HBaseLayerDeleter(attributeStore, instance)

  def apply(attributeStore: HBaseAttributeStore): HBaseLayerDeleter =
    new HBaseLayerDeleter(attributeStore, attributeStore.instance)

  def apply(instance: HBaseInstance): HBaseLayerDeleter =
    apply(HBaseAttributeStore(instance), instance)
}
