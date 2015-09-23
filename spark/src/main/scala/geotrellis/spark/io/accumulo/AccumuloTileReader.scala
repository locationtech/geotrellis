package geotrellis.spark.io.accumulo

import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.{Boundable, KeyBounds, LayerId}
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.{ContainerConstructor, CatalogError, TileNotFoundError, Reader}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.s3._
import geotrellis.spark.io.json._
import org.apache.accumulo.core.data.{Value, Range => ARange}
import org.apache.accumulo.core.security.Authorizations
import org.apache.avro.Schema
import org.apache.hadoop.io.Text
import org.apache.spark.SparkContext
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.collection.JavaConversions._

import scala.reflect.ClassTag

class AccumuloTileReader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
    instance: AccumuloInstance,
    val attributeStore: AccumuloAttributeStore,
    layerId: LayerId)
  extends Reader[K, V] {

  val layerMetaData  = attributeStore.cacheRead[AccumuloLayerMetaData](layerId, Fields.layerMetaData)
  val keyIndex  = attributeStore.cacheRead[KeyIndex[K]](layerId, Fields.keyIndex)
  val writerSchema: Schema = (new Schema.Parser)
    .parse(attributeStore.cacheRead[JsObject](layerId, Fields.schema).toString())
  val codec = KeyValueRecordCodec[K, V]

  val rowId = (index: Long) => new Text(long2Bytes(index))

  def read(key: K): V = {
    val scanner  = instance.connector.createScanner(layerMetaData.tileTable, new Authorizations())
    scanner.setRange(new ARange(rowId(keyIndex.toIndex(key))))
    scanner.fetchColumnFamily(layerId.name)

    val tiles = scanner.iterator
      .map{ entry =>
        AvroEncoder.fromBinary(writerSchema, entry.getValue.get)(codec)
      }
      .flatMap { pairs: Vector[(K, V)] =>
        pairs.filter(pair => pair._1 == key)
      }
      .toVector

    if(tiles.isEmpty) {
      throw new TileNotFoundError(key, layerId)
    } else if(tiles.size > 1) {
      throw new CatalogError(s"Multiple tiles found for $key for layer $layerId")
    } else {
      tiles.head._2
    }
  }
}

object AccumuloTileReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag](
      instance: AccumuloInstance, id: LayerId): AccumuloTileReader[K, V] =
    new AccumuloTileReader[K, V](
      instance = instance,
      attributeStore = AccumuloAttributeStore(instance.connector),
      layerId = id)
}