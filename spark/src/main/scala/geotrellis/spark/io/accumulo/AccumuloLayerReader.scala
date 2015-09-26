package geotrellis.spark.io.accumulo


import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io._
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.accumulo.core.data.{Range => AccumuloRange, Key}
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect._

class AccumuloLayerReader[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, TileType: AvroRecordCodec: ClassTag, Container[_]](
    val attributeStore: AttributeStore.Aux[JsonFormat],
    rddReader: BaseAccumuloRDDReader[K, TileType])
  (implicit sc: SparkContext, val cons: ContainerConstructor[K, TileType, Container])
  extends FilteringLayerReader[LayerId, K, Container[K]] {

  type MetaDataType = cons.MetaDataType

  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RDDQuery[K, MetaDataType], numPartitions: Int) = {
    try {
      val layerMetaData = attributeStore.cacheRead[AccumuloLayerMetaData](id, Fields.layerMetaData)
      val metadata = attributeStore.cacheRead[MetaDataType](id, Fields.rddMetadata)(cons.metaDataFormat)
      val keyBounds = attributeStore.cacheRead[KeyBounds[K]](id, Fields.keyBounds)
      val keyIndex = attributeStore.cacheRead[KeyIndex[K]](id, Fields.keyIndex)
      val writerSchema: Schema = (new Schema.Parser)
        .parse(attributeStore.cacheRead[JsObject](id, Fields.schema).toString())

      val queryKeyBounds = rasterQuery(metadata, keyBounds)

      def indexToKey(index: Long): Key = {
        val none = Array.empty[Byte]
        new Key(long2Bytes(index), none, none, none, Long.MaxValue, true)
      }
      // TODO: Decide if KeyBounds[K] => Range is most useful, or tuple of String, Text, or Array[Byte]
      val decompose = (bounds: KeyBounds[K]) =>
        keyIndex.indexRanges(bounds).map{ case (min, max) =>
          new AccumuloRange(indexToKey(min), true, indexToKey(max), true)
        }

      val rdd = rddReader.read(layerMetaData.tileTable, id.name, writerSchema, queryKeyBounds, decompose)
      cons.makeContainer(rdd, keyBounds, metadata)
    } catch { // TODO: Decide if this is actually helpful, this hides the real error
      case e: AttributeNotFoundError => throw new LayerReadError(id)
    }
  }
}

object AccumuloLayerReader {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, C[_]](instance: AccumuloInstance)
    (implicit sc: SparkContext, cons: ContainerConstructor[K, V, C]): AccumuloLayerReader[K, V, C] =
    new AccumuloLayerReader[K, V, C](
      AccumuloAttributeStore(instance.connector),
      new AccumuloRDDReader[K, V](instance))

}
