package geotrellis.spark.io.hbase

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}

import org.apache.hadoop.hbase.client.Get
import spray.json._

import scala.reflect.ClassTag

class HBaseValueReader(
  instance: HBaseInstance,
  val attributeStore: AttributeStore
) extends ValueReader[LayerId] {

  def reader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val header = attributeStore.readHeader[HBaseLayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)
    val codec = KeyValueRecordCodec[K, V]
    val table = instance.getAdmin.getConnection.getTable(header.tileTable)

    def read(key: K): V = {
      val get = new Get(HBaseKeyEncoder.encode(layerId, keyIndex.toIndex(key)))
      get.addFamily(HBaseRDDWriter.tilesCF)
      val row = table.get(get)
      val tiles: Vector[(K, V)] =
        AvroEncoder
          .fromBinary(writerSchema, row.getValue(HBaseRDDWriter.tilesCF, ""))(codec)
          .filter(pair => pair._1 == key)

      if (tiles.isEmpty) {
        throw new TileNotFoundError(key, layerId)
      } else if (tiles.size > 1) {
        throw new LayerIOError(s"Multiple tiles(${tiles.size}) found for $key for layer $layerId")
      } else {
        tiles.head._2
      }
    }
  }
}

object HBaseValueReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
    instance: HBaseInstance,
    attributeStore: AttributeStore,
    layerId: LayerId
  ): Reader[K, V] =
    new HBaseValueReader(instance, attributeStore).reader[K, V](layerId)

  def apply(instance: HBaseInstance): HBaseValueReader =
    new HBaseValueReader(
      instance = instance,
      attributeStore = HBaseAttributeStore(instance))

  def apply(attributeStore: HBaseAttributeStore): HBaseValueReader =
    new HBaseValueReader(
      instance = attributeStore.instance,
      attributeStore = attributeStore)
}
