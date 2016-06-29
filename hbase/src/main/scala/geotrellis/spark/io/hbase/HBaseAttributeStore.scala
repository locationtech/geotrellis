package geotrellis.spark.io.hbase

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.Logging
import spray.json._

import scala.collection.JavaConversions._

abstract class HBaseAttributeStore(val admin: Admin, val attributeTable: String) extends DiscreteLayerAttributeStore with Logging {

  //create the attribute table if it does not exist
  {
    if (!admin.tableExists(attributeTable)) {
      val tableDesc: HTableDescriptor = new HTableDescriptor(attributeTable: TableName)
      val idsColumnFamilyDesc = new HColumnDescriptor("name")
      tableDesc.addFamily(idsColumnFamilyDesc)

      admin.createTable(tableDesc)
    }
  }

  val table = admin.getConnection.getTable(attributeTable)

  val SEP = "__.__"

  def layerIdString(layerId: LayerId): String =
    s"${layerId.name}${SEP}${layerId.zoom}"

  private def fetch(layerId: Option[LayerId], attributeName: String): Iterator[Result] = {
    val scan = new Scan()
    layerId.foreach { id =>
      scan.setStartRow(layerIdString(id))
      scan.setStopRow(layerIdString(id))
    }
    scan.addFamily(attributeName)
    table.getScanner(scan).iterator()
  }

  private def delete(layerId: LayerId, attributeName: Option[String]): Unit = {
    if(!layerExists(layerId)) throw new LayerNotFoundError(layerId)

    val delete = new Delete(layerIdString(layerId))
    attributeName.foreach(delete.addFamily(_))
    table.delete(Delete)

    attributeName match {
      case Some(attribute) => clearCache(layerId, attribute)
      case None => clearCache(layerId)
    }
  }

  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T = {
    val values = fetch(Some(layerId), attributeName).map(_.rawCells()).toVector.flatten

    if(values.isEmpty) {
      throw new AttributeNotFoundError(attributeName, layerId)
    } else if(values.size > 1) {
      throw new LayerIOError(s"Multiple attributes found for $attributeName for layer $layerId")
    } else {
      Bytes.toString(values.head.getValueArray).parseJson.convertTo[(LayerId, T)]._2
    }
  }

  def readAll[T: JsonFormat](attributeName: String): Map[LayerId, T] = {
    fetch(None, attributeName).map(_.rawCells()).toVector.flatten
      .map { row => Bytes.toString(row.getValueArray).parseJson.convertTo[(LayerId, T)] }
      .toMap
  }

  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    val put = new Put(layerIdString(layerId))
    put.addColumn(
      attributeName, "", System.currentTimeMillis(),
      (layerId, value).toJson.compactPrint.getBytes
    )

    table.put(put)
  }

  def layerExists(layerId: LayerId): Boolean = {
    val id = layerIdString(layerId)
    val scan = new Scan()
    scan.setStartRow(id)
    scan.setStopRow(id)
    table.getScanner(scan).iterator()
      .exists { kv =>
        val List(name, zoomStr) = Bytes.toString(kv.getRow).split(SEP).toList
        layerId == LayerId(name, zoomStr.toInt)
      }
  }

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

  def availableAttributes(layerId: LayerId): Seq[String] = {
    val id = layerIdString(layerId)
    val scan = new Scan()
    scan.setStartRow(id)
    scan.setStopRow(id)
    table.getScanner(scan).iterator().map(_.rawCells()).toVector.flatten.map { row =>
      Bytes.toString(row.getFamilyArray)
    }
  }
}
