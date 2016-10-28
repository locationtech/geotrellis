package geotrellis.spark.io.hbase

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.util._
import org.apache.spark.SparkContext
import spray.json._

import scala.reflect._

class HBaseCollectionLayerReader(val attributeStore: AttributeStore, instance: HBaseInstance)
  extends CollectionLayerReader[LayerId] {

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], filterIndexOnly: Boolean) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HBaseLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = rasterQuery(metadata)

    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)

    val seq = HBaseCollectionReader.read[K, V](instance, header.tileTable, id, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))
    new ContextCollection(seq, metadata)
  }
}

object HBaseCollectionLayerReader {
  def apply(instance: HBaseInstance): HBaseCollectionLayerReader =
    new HBaseCollectionLayerReader(HBaseAttributeStore(instance), instance)

  def apply(attributeStore: HBaseAttributeStore): HBaseCollectionLayerReader =
    new HBaseCollectionLayerReader(attributeStore, attributeStore.instance)
}
