package geotrellis.spark.io.hbase

import com.typesafe.config.ConfigFactory
import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.Logging
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.collection.JavaConversions._

object HBaseAttributeStore {
  def apply(instance: HBaseInstance): HBaseAttributeStore =
    new HBaseAttributeStore(instance, ConfigFactory.load().getString("geotrellis.hbase.catalog"))
  def apply(instance: HBaseInstance, attributeTable: String): HBaseAttributeStore =
    new HBaseAttributeStore(instance, attributeTable)
}

class HBaseAttributeStore(val instance: HBaseInstance, val attributeTable: String) extends DiscreteLayerAttributeStore with Logging {

  //create the attribute table if it does not exist
  if (!instance.getAdmin.tableExists(attributeTable)) {
    val tableDesc = new HTableDescriptor(attributeTable: TableName)
    val headerColumnFamilyDesc = new HColumnDescriptor(AttributeStore.Fields.header)
    tableDesc.addFamily(headerColumnFamilyDesc)
    instance.getAdmin.createTable(tableDesc)
  }

  val table = instance.getAdmin.getConnection.getTable(attributeTable)

  val SEP = HBaseRDDWriter.SEP

  def layerIdString(layerId: LayerId): String = s"${layerId.name}${SEP}${layerId.zoom}"

  def addColumn(cf: String) = if(!table.getTableDescriptor.hasFamily(cf))
    instance.getAdmin.addColumn(attributeTable, new HColumnDescriptor(cf))

  private def fetch(layerId: Option[LayerId], attributeName: String): Iterator[Result] = {
    val scan = new Scan()
    layerId.foreach { id =>
      scan.setStartRow(layerIdString(id))
      scan.setStopRow(stringToBytes(layerIdString(id)) :+ 0.toByte) // add trailing byte, to include stop row
    }
    scan.addFamily(attributeName)
    table.getScanner(scan).iterator()
  }

  private def delete(layerId: LayerId, attributeName: Option[String]): Unit = {
    if(!layerExists(layerId)) throw new LayerNotFoundError(layerId)

    val delete = new Delete(layerIdString(layerId))
    attributeName.foreach(delete.addFamily(_))
    table.delete(delete)
    attributeName.foreach(table.getTableDescriptor.removeFamily(_))

    attributeName match {
      case Some(attribute) => clearCache(layerId, attribute)
      case None => clearCache(layerId)
    }
  }

  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T = {
    val values = fetch(Some(layerId), attributeName).toVector

    if(values.isEmpty) {
      throw new AttributeNotFoundError(attributeName, layerId)
    } else if(values.size > 1) {
      throw new LayerIOError(s"Multiple attributes found for $attributeName for layer $layerId")
    } else {
      Bytes.toString(values.head.getValue(attributeName, "")).parseJson.convertTo[(LayerId, T)]._2
    }
  }

  def readAll[T: JsonFormat](attributeName: String): Map[LayerId, T] = {
    fetch(None, attributeName).toVector
      .map { row => Bytes.toString(row.getValue(attributeName, "")).parseJson.convertTo[(LayerId, T)] }
      .toMap
  }

  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    addColumn(attributeName)
    val put = new Put(layerIdString(layerId))
    put.addColumn(
      attributeName, "", System.currentTimeMillis(),
      (layerId, value).toJson.compactPrint.getBytes
    )

    table.put(put)
  }

  def layerExists(layerId: LayerId): Boolean = !table.get(new Get(layerIdString(layerId))).isEmpty

  def delete(layerId: LayerId): Unit = delete(layerId, None)

  def delete(layerId: LayerId, attributeName: String): Unit = delete(layerId, Some(attributeName))

  def layerIds: Seq[LayerId] = {
    val scan = new Scan()
    table.getScanner(scan).iterator()
      .map { kv: Result =>
        val List(name, zoomStr) = Bytes.toString(kv.getRow).split(SEP).toList
        LayerId(name, zoomStr.toInt)
      }
      .toList
      .distinct
  }

  def availableAttributes(layerId: LayerId): Seq[String] =
    table.getTableDescriptor.getFamiliesKeys.map(Bytes.toString).toSeq
}
