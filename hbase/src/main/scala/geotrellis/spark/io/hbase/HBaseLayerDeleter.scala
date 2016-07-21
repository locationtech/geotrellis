package geotrellis.spark.io.hbase

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import org.apache.hadoop.hbase.client._

import scala.collection.JavaConversions._

class HBaseLayerDeleter(val attributeStore: AttributeStore, instance: HBaseInstance) extends LayerDeleter[LayerId] {

  def delete(id: LayerId): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val header = try {
      attributeStore.readHeader[HBaseLayerHeader](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(id).initCause(e)
    }

    val table = instance.getAdmin.getConnection.getTable(header.tileTable)

    // Deletion list should be mutable
    val list = new java.util.ArrayList[Delete]()
    val scan = new Scan()
    scan.addColumn(id.name, id.zoom)

    table.getScanner(scan).iterator().foreach { kv =>
      val delete = new Delete(kv.getRow)
      delete.addColumn(id.name, id.zoom)
      list.add(delete)
    }

    table.delete(list)
    instance.getAdmin.deleteColumn(header.tileTable, id.name)

    attributeStore.delete(id)
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
