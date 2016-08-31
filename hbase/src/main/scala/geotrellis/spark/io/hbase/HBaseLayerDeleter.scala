package geotrellis.spark.io.hbase

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter.PrefixFilter
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._

class HBaseLayerDeleter(val attributeStore: AttributeStore, instance: HBaseInstance) extends LayerDeleter[LayerId] {

  def delete(id: LayerId): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val header = try {
      attributeStore.readHeader[HBaseLayerHeader](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(id).initCause(e)
    }

    // Deletion list should be mutable
    val list = new java.util.ArrayList[Delete]()
    val scan = new Scan()
    scan.addFamily(HBaseRDDWriter.tilesCF)
    scan.setFilter(new PrefixFilter(HBaseRDDWriter.layerIdString(id)))

    instance.withTableConnectionDo(header.tileTable) { table =>
      val scanner = table.getScanner(scan)
      try {
        scanner.iterator().foreach { kv =>
          val delete = new Delete(kv.getRow)
          delete.addFamily(HBaseRDDWriter.tilesCF)
          list.add(delete)
        }
      } finally scanner.close()

      table.delete(list)
    }

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
