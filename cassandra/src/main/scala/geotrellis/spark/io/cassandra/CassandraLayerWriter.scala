package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.util._

import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class CassandraLayerWriter(
  val attributeStore: AttributeStore,
  instance: CassandraInstance,
  table: String
) extends LayerWriter[LayerId] {

  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    val header =
      CassandraLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        tileTable = table
      )
    val metadata = rdd.metadata
    val encodeKey = (key: K) => (keyIndex.toIndex(key), id)

    /*// If no table exists, add the table and set the splits according to the
    // key index's keybounds and the number of partitions in the RDD.
    // This is a "best guess" scenario; users should use CassandraUtils to
    // manually create splits based on their cluster configuration for best
    // performance.
    val ops = instance.connector.tableOperations()
    if (!ops.exists(table)) {
      ops.create(table)
      CassandraUtils.addSplits(table, instance, keyIndex.keyBounds, keyIndex, rdd.partitions.length)
    }*/

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, schema)
      CassandraRDDWriter.write(rdd, instance, encodeKey, table)

      /*// Create locality groups based on encoding strategy
      for(lg <- CassandraKeyEncoder.getLocalityGroups(id)) {
        instance.makeLocalityGroup(table, lg)
      }*/
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object CassandraLayerWriter {
  def apply(
    instance: CassandraInstance,
    table: String
  ): CassandraLayerWriter =
    new CassandraLayerWriter(
      attributeStore = CassandraAttributeStore(instance),
      instance = instance,
      table = table
    )

  def apply(
    instance: CassandraInstance,
    attributeStore: AttributeStore,
    table: String
  ): CassandraLayerWriter =
    new CassandraLayerWriter(
      attributeStore = attributeStore,
      instance = instance,
      table = table
    )
}
