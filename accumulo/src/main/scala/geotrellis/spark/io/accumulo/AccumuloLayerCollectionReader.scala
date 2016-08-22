package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.util._

import org.apache.accumulo.core.data.{Range => AccumuloRange}
import org.apache.hadoop.io.Text
import spray.json._

import scala.reflect._

class AccumuloLayerCollectionReader(val attributeStore: AttributeStore)(implicit instance: AccumuloInstance) extends CollectionLayerReader[LayerId] {

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], filterIndexOnly: Boolean) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = rasterQuery(metadata)

    val decompose = (bounds: KeyBounds[K]) =>
      keyIndex.indexRanges(bounds).map { case (min, max) =>
        new AccumuloRange(new Text(AccumuloKeyEncoder.long2Bytes(min)), new Text(AccumuloKeyEncoder.long2Bytes(max)))
      }

    val seq = AccumuloCollectionReader.read[K, V](header.tileTable, columnFamily(id), queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))
    new ContextCollection(seq, metadata)
  }
}

object AccumuloLayerCollectionReader {
  def apply(attributeStore: AccumuloAttributeStore)(implicit instance: AccumuloInstance): AccumuloLayerCollectionReader =
    new AccumuloLayerCollectionReader(attributeStore)

  def apply(implicit instance: AccumuloInstance): AccumuloLayerCollectionReader =
    new AccumuloLayerCollectionReader(AccumuloAttributeStore(instance.connector))
}
