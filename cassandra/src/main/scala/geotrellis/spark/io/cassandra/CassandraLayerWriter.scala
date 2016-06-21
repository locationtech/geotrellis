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
  keyspace: String,
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
        keyspace = keyspace,
        tileTable = table
      )
    val metadata = rdd.metadata
    val encodeKey = (key: K) => keyIndex.toIndex(key)

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, schema)
      CassandraRDDWriter.write(rdd, instance, id, encodeKey, keyspace, table)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object CassandraLayerWriter {
  def apply(
    instance: CassandraInstance,
    keyspace: String,
    table: String
  ): CassandraLayerWriter =
    new CassandraLayerWriter(
      attributeStore = CassandraAttributeStore(instance),
      instance = instance,
      keyspace = keyspace,
      table = table
    )

  def apply(
    attributeStore: CassandraAttributeStore,
    keyspace: String,
    table: String
  ): CassandraLayerWriter =
    new CassandraLayerWriter(
      attributeStore = attributeStore,
      instance = attributeStore.instance,
      keyspace = keyspace,
      table = table
    )
}
