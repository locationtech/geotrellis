package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.util._

import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class AccumuloLayerWriter(
  val attributeStore: AttributeStore,
  instance: AccumuloInstance,
  table: String,
  options: AccumuloLayerWriter.Options
) extends LayerWriter[LayerId] {

  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    val header =
      AccumuloLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        tileTable = table
      )
    val metadata = rdd.metadata
    val encodeKey = (key: K) => AccumuloKeyEncoder.encode(id, key, keyIndex.toIndex(key))

    // If no table exists, add the table and set the splits according to the
    // key index's keybounds and the number of partitions in the RDD.
    // This is a "best guess" scenario; users should use AccumuloUtils to
    // manually create splits based on their cluster configuration for best
    // performance.
    val ops = instance.connector.tableOperations()
    if (!ops.exists(table)) {
      ops.create(table)
      AccumuloUtils.addSplits(table, instance, keyIndex.keyBounds, keyIndex, rdd.partitions.length)
    }

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, schema)
      AccumuloRDDWriter.write(rdd, instance, encodeKey, options.writeStrategy, table)

      // Create locality groups based on encoding strategy
      for(lg <- AccumuloKeyEncoder.getLocalityGroups(id)) {
        instance.makeLocalityGroup(table, lg)
      }
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object AccumuloLayerWriter {
  case class Options(
    writeStrategy: AccumuloWriteStrategy = AccumuloWriteStrategy.DEFAULT
  )

  object Options {
    def DEFAULT = Options()

    implicit def writeStrategyToOptions(ws: AccumuloWriteStrategy): Options =
      Options(writeStrategy = ws)
  }

  def apply(
    instance: AccumuloInstance,
    table: String,
    options: Options
  ): AccumuloLayerWriter =
    new AccumuloLayerWriter(
      attributeStore = AccumuloAttributeStore(instance.connector),
      instance = instance,
      table = table,
      options = options
    )

  def apply(
    instance: AccumuloInstance,
    table: String
  ): AccumuloLayerWriter =
    new AccumuloLayerWriter(
      attributeStore = AccumuloAttributeStore(instance.connector),
      instance = instance,
      table = table,
      options = Options.DEFAULT
    )

  def apply(
    instance: AccumuloInstance,
    attributeStore: AttributeStore,
    table: String,
    options: Options
  ): AccumuloLayerWriter =
    new AccumuloLayerWriter(
      attributeStore = attributeStore,
      instance = instance,
      table = table,
      options = options
    )

  def apply(
    instance: AccumuloInstance,
    attributeStore: AttributeStore,
    table: String
  ): AccumuloLayerWriter =
    new AccumuloLayerWriter(
      attributeStore = attributeStore,
      instance = instance,
      table = table,
      options = Options.DEFAULT
    )
}
