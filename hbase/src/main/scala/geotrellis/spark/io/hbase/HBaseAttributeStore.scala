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

package geotrellis.spark.io.hbase

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hbase.conf.HBaseConfig

import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.collection.JavaConversions._

object HBaseAttributeStore {
  def apply(instance: HBaseInstance): HBaseAttributeStore =
    new HBaseAttributeStore(instance, HBaseConfig.catalog)
  def apply(instance: HBaseInstance, attributeTable: String): HBaseAttributeStore =
    new HBaseAttributeStore(instance, attributeTable)
}

class HBaseAttributeStore(val instance: HBaseInstance, val attributeTable: String) extends DiscreteLayerAttributeStore {

  private val attributeTableName: TableName = attributeTable

  //create the attribute table if it does not exist
  instance.withAdminDo { admin =>
    if (!admin.tableExists(attributeTableName)) {
      val tableDesc = new HTableDescriptor(attributeTableName)
      val headerColumnFamilyDesc = new HColumnDescriptor(AttributeStore.Fields.header)
      tableDesc.addFamily(headerColumnFamilyDesc)
      admin.createTable(tableDesc)
    }
  }

  val SEP = HBaseRDDWriter.SEP

  def layerIdString(layerId: LayerId): String = s"${layerId.name}${SEP}${layerId.zoom}"

  private def addColumn(table: Table)(cf: String) =
    if(!table.getTableDescriptor.hasFamily(cf))
      instance.getAdmin.addColumn(attributeTableName, new HColumnDescriptor(cf))

  private def fetch(layerId: Option[LayerId], attributeName: String): Vector[Result] =
    instance.withTableConnectionDo(attributeTableName) { table =>
      if (table.getTableDescriptor.hasFamily(attributeName)) {
        val scan = new Scan()
        layerId.foreach { id =>
          scan.withStartRow(layerIdString(id), true)
          scan.withStopRow(stringToBytes(layerIdString(id)), true)
        }
        scan.addFamily(attributeName)
        val scanner = table.getScanner(scan)
        try scanner.iterator().toVector finally scanner.close()
      } else Vector()
    }

  private def delete(layerId: LayerId, attributeName: Option[String]): Unit =
    instance.withTableConnectionDo(attributeTableName) { table =>
      val delete = new Delete(layerIdString(layerId))
      attributeName.foreach(delete.addFamily(_))
      table.delete(delete)

      attributeName match {
        case Some(attribute) =>
          table.getTableDescriptor.removeFamily(attribute)
          clearCache(layerId, attribute)
        case None =>
          clearCache(layerId)
      }
    }

  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T = {
      val values = fetch(Some(layerId), attributeName)

      if (values.isEmpty) {
        throw new AttributeNotFoundError(attributeName, layerId)
      } else if (values.size > 1) {
        throw new LayerIOError(s"Multiple attributes found for $attributeName for layer $layerId")
      } else {
        Bytes.toString(values.head.getValue(attributeName, "")).parseJson.convertTo[(LayerId, T)]._2
      }
    }

  def readAll[T: JsonFormat](attributeName: String): Map[LayerId, T] = {
      fetch(None, attributeName)
        .map { row => Bytes.toString(row.getValue(attributeName, "")).parseJson.convertTo[(LayerId, T)] }
        .toMap
    }

  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit =
    instance.withTableConnectionDo(attributeTableName) { table =>
      addColumn(table)(attributeName)

      val put = new Put(layerIdString(layerId))
      put.addColumn(
        attributeName, "", System.currentTimeMillis(),
        (layerId, value).toJson.compactPrint.getBytes
      )

      table.put(put)
    }

  def layerExists(layerId: LayerId): Boolean =
    fetch(Some(layerId), AttributeStore.Fields.metadata).nonEmpty

  def delete(layerId: LayerId): Unit = delete(layerId, None)

  def delete(layerId: LayerId, attributeName: String): Unit = delete(layerId, Some(attributeName))

  def layerIds: Seq[LayerId] = instance.withTableConnectionDo(attributeTableName) { table =>
    val scanner = table.getScanner(new Scan())
    try {
      scanner.iterator()
        .map { kv: Result =>
          val List(name, zoomStr) = Bytes.toString(kv.getRow).split(SEP).toList
          LayerId(name, zoomStr.toInt)
        }
        .toList
        .distinct
    } finally scanner.close()
  }

  def availableAttributes(layerId: LayerId): Seq[String] = instance.withTableConnectionDo(attributeTableName) {
    _.getTableDescriptor.getFamiliesKeys.map(Bytes.toString).toSeq
  }
}
